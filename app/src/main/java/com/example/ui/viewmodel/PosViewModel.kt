package com.example.ui.viewmodel

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Product
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class PosViewModel(private val repository: PosRepository) : ViewModel() {

    // 1. Products flow
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Invoices flow
    val invoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2b. Invoice Items flow
    val invoiceItems: StateFlow<List<InvoiceItem>> = repository.getAllInvoiceItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Current active cart (Product -> Quantity)
    private val _cart = MutableStateFlow<Map<Product, Int>>(emptyMap())
    val cart: StateFlow<Map<Product, Int>> = _cart.asStateFlow()

    // 4. Barcode scanning scan cooldown
    private var lastScanTime = 0L
    private val scanCooldownMs = 1000L // 1 second cooldown

    // 5. Sound feedback (ToneGenerator)
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.e("PosViewModel", "Failed to initialize ToneGenerator", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator?.release()
    }

    // Play POS Beep tone
    fun playBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            Log.e("PosViewModel", "Failed to play beep sound", e)
        }
    }

    // Handles scanned QR/Barcode
    fun handleScannedBarcode(barcode: String, onScannedSuccessfully: (String) -> Unit = {}) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < scanCooldownMs) {
            // Under Cooldown (Debounce)
            return
        }
        lastScanTime = currentTime

        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
                playBeep()
                addProductToCart(product)
                onScannedSuccessfully("تم مسح: ${product.name}")
            } else {
                // If product is not found, play error tone or handle gracefully (user should be notified to add it)
                onScannedSuccessfully("المنتج ذو الكود $barcode غير متوفر! يرجى إضافته أولاً.")
            }
        }
    }

    // Cart management
    fun addProductToCart(product: Product) {
        val current = _cart.value.toMutableMap()
        if (current.containsKey(product)) {
            // Note: The user requested that camera scan DOES NOT increment quantity,
            // but manual (+) button does.
            // When we manually/programmatically call addProductToCart (e.g. from listing/quick add),
            // we initialize as 1 or do nothing if already exists.
            // "عند مسح المنتج، تتم إضافته للفاتورة بـ (كمية = 1). إذا تم مسح نفس المنتج بالكاميرا مرة أخرى، لا تضف الكمية تلقائياً لمنع الأخطاء."
            // So if product already exists, we do NOT change quantity!
        } else {
            current[product] = 1
        }
        _cart.value = current
    }

    fun incrementCartItem(product: Product) {
        val current = _cart.value.toMutableMap()
        val qty = current[product] ?: 0
        current[product] = qty + 1
        _cart.value = current
    }

    fun decrementCartItem(product: Product) {
        val current = _cart.value.toMutableMap()
        val qty = current[product] ?: 0
        if (qty <= 1) {
            current.remove(product)
        } else {
            current[product] = qty - 1
        }
        _cart.value = current
    }

    fun removeFromCart(product: Product) {
        val current = _cart.value.toMutableMap()
        current.remove(product)
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    // Calculations
    val cartTotal: Double
        get() = _cart.value.entries.sumOf { it.key.salePrice * it.value }

    val cartTotalProfit: Double
        get() = _cart.value.entries.sumOf { (it.key.salePrice - it.key.purchasePrice) * it.value }

    // Checkout: Complete Sale (Paid immediately)
    fun completeSaleAndCheckout(onCompleted: () -> Unit) {
        if (_cart.value.isEmpty()) return

        val total = cartTotal
        val profit = cartTotalProfit
        val itemsToSave = _cart.value.map { (product, quantity) ->
            InvoiceItem(
                invoiceId = 0, // Assigned inside Repository
                productBarcode = product.barcode,
                productName = product.name,
                purchasePrice = product.purchasePrice,
                salePrice = product.salePrice,
                quantity = quantity
            )
        }

        viewModelScope.launch {
            val invoice = Invoice(
                timestamp = System.currentTimeMillis(),
                totalAmount = total,
                isPaid = true,
                totalProfit = profit,
                customerName = null
            )
            repository.insertInvoice(invoice, itemsToSave)
            clearCart()
            onCompleted()
        }
    }

    // Checkout: Credit sale ("الكريدي")
    fun checkoutWithCredit(customerName: String, onCompleted: () -> Unit) {
        if (_cart.value.isEmpty() || customerName.trim().isEmpty()) return

        val total = cartTotal
        val profit = cartTotalProfit
        val itemsToSave = _cart.value.map { (product, quantity) ->
            InvoiceItem(
                invoiceId = 0,
                productBarcode = product.barcode,
                productName = product.name,
                purchasePrice = product.purchasePrice,
                salePrice = product.salePrice,
                quantity = quantity
            )
        }

        viewModelScope.launch {
            val invoice = Invoice(
                timestamp = System.currentTimeMillis(),
                totalAmount = total,
                isPaid = false,
                totalProfit = profit,
                customerName = customerName.trim()
            )
            repository.insertInvoice(invoice, itemsToSave)
            clearCart()
            onCompleted()
        }
    }

    // Delete Invoice (Recalculates totals instantly)
    fun deleteInvoice(invoiceId: Int) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceId)
        }
    }

    // Customer Debt repayment ("تم السداد")
    fun settleCustomerDebts(customerName: String) {
        viewModelScope.launch {
            repository.markCustomerAsPaid(customerName)
        }
    }

    // Partial Debt settlement ("تسديد جزئي")
    fun payPartialDebt(customerName: String, amountToPay: Double, onResult: (Boolean, String) -> Unit = {_,_ ->}) {
        if (customerName.trim().isEmpty() || amountToPay <= 0.0) {
            onResult(false, "القيمة غير صالحة")
            return
        }
        viewModelScope.launch {
            try {
                // Get all unpaid invoices for this customer, oldest first
                val customerUnpaid = invoices.value
                    .filter { !it.isPaid && it.customerName?.equals(customerName.trim(), ignoreCase = true) == true }
                    .sortedBy { it.timestamp }

                var remainingPayment = amountToPay
                val currentTime = System.currentTimeMillis()

                for (invoice in customerUnpaid) {
                    if (remainingPayment <= 0.0) break

                    if (remainingPayment >= invoice.totalAmount) {
                        // Mark this invoice as fully paid
                        remainingPayment -= invoice.totalAmount
                        val updatedInvoice = invoice.copy(
                            isPaid = true,
                            timestamp = currentTime
                        )
                        repository.insertInvoiceDirectly(updatedInvoice)
                    } else {
                        // Partially pay this invoice by splitting it
                        val originalAmount = invoice.totalAmount
                        val amountPaid = remainingPayment
                        val amountUnpaid = originalAmount - amountPaid

                        val ratio = amountPaid / originalAmount
                        val paidProfit = invoice.totalProfit * ratio
                        val unpaidProfit = invoice.totalProfit - paidProfit

                        // Update existing unpaid invoice with the remainder
                        val updatedUnpaidInvoice = invoice.copy(
                            totalAmount = amountUnpaid,
                            totalProfit = unpaidProfit
                        )
                        repository.insertInvoiceDirectly(updatedUnpaidInvoice)

                        // Create new paid invoice for the paid part to record in revenues
                        val paidInvoicePart = Invoice(
                            timestamp = currentTime,
                            totalAmount = amountPaid,
                            isPaid = true,
                            totalProfit = paidProfit,
                            customerName = customerName.trim()
                        )
                        repository.insertInvoiceDirectly(paidInvoicePart)

                        remainingPayment = 0.0
                    }
                }
                onResult(true, "تم تسديد $amountToPay د.ت بنجاح!")
            } catch (e: Exception) {
                Log.e("PosViewModel", "Failed to pay partial debt", e)
                onResult(false, "فشلت عملية السداد الجزئي.")
            }
        }
    }

    // Update Product Info
    fun updateProduct(product: Product, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.insertProduct(product)
            onResult(true)
        }
    }

    // Add Product
    fun addNewProduct(barcode: String, name: String, purchasePrice: Double, salePrice: Double, imageUrl: String? = null, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getProductByBarcode(barcode)
            if (existing != null) {
                // If it already exists, replace/overwrite it
                repository.insertProduct(
                    existing.copy(
                        name = name,
                        purchasePrice = purchasePrice,
                        salePrice = salePrice,
                        imageUrl = imageUrl
                    )
                )
            } else {
                repository.insertProduct(
                    Product(
                        barcode = barcode,
                        name = name,
                        purchasePrice = purchasePrice,
                        salePrice = salePrice,
                        imageUrl = imageUrl
                    )
                )
            }
            onResult(true)
        }
    }

    // Delete Product
    fun deleteProductById(productId: Int) {
        viewModelScope.launch {
            repository.deleteProductById(productId)
        }
    }

    // Exports all database entries to a single Base64 encoded JSON string
    fun exportBackupCode(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentProducts = products.value
                val currentInvoices = invoices.value
                val currentItems = repository.getAllInvoiceItems()

                val jsonRoot = JSONObject()

                // 1. Pack Products
                val pArray = JSONArray()
                for (p in currentProducts) {
                    val pObj = JSONObject().apply {
                        put("barcode", p.barcode)
                        put("name", p.name)
                        put("purchasePrice", p.purchasePrice)
                        put("salePrice", p.salePrice)
                        put("imageUrl", p.imageUrl ?: "")
                    }
                    pArray.put(pObj)
                }
                jsonRoot.put("products", pArray)

                // 2. Pack Invoices
                val invArray = JSONArray()
                for (i in currentInvoices) {
                    val iObj = JSONObject().apply {
                        put("id", i.id)
                        put("timestamp", i.timestamp)
                        put("totalAmount", i.totalAmount)
                        put("isPaid", i.isPaid)
                        put("customerName", i.customerName ?: "")
                        put("totalProfit", i.totalProfit)
                    }
                    invArray.put(iObj)
                }
                jsonRoot.put("invoices", invArray)

                // 3. Pack Invoice Items
                val itemArray = JSONArray()
                for (item in currentItems) {
                    val itemObj = JSONObject().apply {
                        put("invoiceId", item.invoiceId)
                        put("productBarcode", item.productBarcode)
                        put("productName", item.productName)
                        put("purchasePrice", item.purchasePrice)
                        put("salePrice", item.salePrice)
                        put("quantity", item.quantity)
                    }
                    itemArray.put(itemObj)
                }
                jsonRoot.put("invoiceItems", itemArray)

                val jsonStr = jsonRoot.toString()
                val base64Code = Base64.encodeToString(jsonStr.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                onComplete(base64Code)
            } catch (e: Exception) {
                Log.e("PosViewModel", "Export failed", e)
                onComplete("")
            }
        }
    }

    // Decodes and restores database entries from Base64 JSON and overwrites local tables
    fun importBackupCode(backupCode: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (backupCode.trim().isEmpty()) {
                    onComplete(false, "الرجاء إدخال كود نسخ احتياطي صالح!")
                    return@launch
                }
                val decodedBytes = Base64.decode(backupCode.trim(), Base64.NO_WRAP)
                val jsonStr = String(decodedBytes, Charsets.UTF_8)
                val jsonRoot = JSONObject(jsonStr)

                // Clear everything first
                repository.clearAllData()

                // 1. Restore products
                val pArray = jsonRoot.optJSONArray("products")
                if (pArray != null) {
                    for (i in 0 until pArray.length()) {
                        val pObj = pArray.getJSONObject(i)
                        repository.insertProduct(
                            Product(
                                barcode = pObj.getString("barcode"),
                                name = pObj.getString("name"),
                                purchasePrice = pObj.getDouble("purchasePrice"),
                                salePrice = pObj.getDouble("salePrice"),
                                imageUrl = if (pObj.has("imageUrl") && pObj.getString("imageUrl").isNotEmpty()) pObj.getString("imageUrl") else null
                            )
                        )
                    }
                }

                // 2. Restore invoices
                val invArray = jsonRoot.optJSONArray("invoices")
                if (invArray != null) {
                    for (i in 0 until invArray.length()) {
                        val iObj = invArray.getJSONObject(i)
                        repository.insertInvoiceDirectly(
                            Invoice(
                                id = iObj.getInt("id"),
                                timestamp = iObj.getLong("timestamp"),
                                totalAmount = iObj.getDouble("totalAmount"),
                                isPaid = iObj.getBoolean("isPaid"),
                                customerName = if (iObj.has("customerName") && iObj.getString("customerName").isNotEmpty()) iObj.getString("customerName") else null,
                                totalProfit = iObj.getDouble("totalProfit")
                            )
                        )
                    }
                }

                // 3. Restore invoice items
                val itemArray = jsonRoot.optJSONArray("invoiceItems")
                if (itemArray != null) {
                    for (i in 0 until itemArray.length()) {
                        val itemObj = itemArray.getJSONObject(i)
                        repository.insertInvoiceItemDirectly(
                            InvoiceItem(
                                invoiceId = itemObj.getInt("invoiceId"),
                                productBarcode = itemObj.getString("productBarcode"),
                                productName = itemObj.getString("productName"),
                                purchasePrice = itemObj.getDouble("purchasePrice"),
                                salePrice = itemObj.getDouble("salePrice"),
                                quantity = itemObj.getInt("quantity")
                            )
                        )
                    }
                }

                onComplete(true, "تمت استعادة كافة البيانات (${pArray?.length() ?: 0} منتجات، ${invArray?.length() ?: 0} فواتير) بنجاح!")
            } catch (e: Exception) {
                Log.e("PosViewModel", "Import failed", e)
                onComplete(false, "فشل الاستعادة: نسق الكود تالف أو غير مدعوم!")
            }
        }
    }

    // Direct manual debt calculation/addition for clients
    fun addManualDebt(customerName: String, amount: Double, onResult: (Boolean) -> Unit) {
        addManualDebtWithDetails(customerName, amount, "", onResult)
    }

    // Direct manual debt with comments/details
    fun addManualDebtWithDetails(customerName: String, amount: Double, details: String, onResult: (Boolean) -> Unit) {
        if (customerName.trim().isEmpty() || amount <= 0.0) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val invoice = Invoice(
                timestamp = System.currentTimeMillis(),
                totalAmount = amount,
                isPaid = false,
                totalProfit = 0.0, // Manual custom debts are registered with zero default profit
                customerName = customerName.trim()
            )
            val itemsToSave = if (details.trim().isNotEmpty()) {
                listOf(
                    InvoiceItem(
                        invoiceId = 0,
                        productBarcode = "MANUAL",
                        productName = details.trim(),
                        purchasePrice = 0.0,
                        salePrice = amount,
                        quantity = 1
                    )
                )
            } else {
                emptyList()
            }
            // Save inside database as an unpaid transaction under the customer's name
            repository.insertInvoice(invoice, itemsToSave)
            onResult(true)
        }
    }
}
