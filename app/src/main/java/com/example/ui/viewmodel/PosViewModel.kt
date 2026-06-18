package com.example.ui.viewmodel

import android.media.AudioManager
import android.media.ToneGenerator
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

class PosViewModel(private val repository: PosRepository) : ViewModel() {

    // 1. Products flow
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Invoices flow
    val invoices: StateFlow<List<Invoice>> = repository.allInvoices
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
}
