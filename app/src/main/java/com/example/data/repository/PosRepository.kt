package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow

class PosRepository(private val db: AppDatabase) {
    private val productDao = db.productDao()
    private val invoiceDao = db.invoiceDao()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()

    suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)
    }

    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun deleteProductById(id: Int) {
        productDao.deleteProductById(id)
    }

    suspend fun insertInvoice(invoice: Invoice, items: List<InvoiceItem>) {
        // We will insert the invoice, get its auto-generated ID, and save the item records
        val invoiceIdLong = invoiceDao.insertInvoice(invoice)
        val invoiceId = invoiceIdLong.toInt()
        for (item in items) {
            val itemWithInvoiceId = item.copy(invoiceId = invoiceId)
            invoiceDao.insertInvoiceItem(itemWithInvoiceId)
        }
    }

    suspend fun deleteInvoice(invoiceId: Int) {
        // Deletes the invoice record and all its elements to clean up numbers
        invoiceDao.deleteInvoiceById(invoiceId)
        invoiceDao.deleteInvoiceItemsByInvoiceId(invoiceId)
    }

    suspend fun getInvoiceItems(invoiceId: Int): List<InvoiceItem> {
        return invoiceDao.getInvoiceItems(invoiceId)
    }

    fun getInvoiceItemsFlow(invoiceId: Int): Flow<List<InvoiceItem>> {
        return invoiceDao.getInvoiceItemsFlow(invoiceId)
    }

    suspend fun markCustomerAsPaid(customerName: String) {
        val currentTime = System.currentTimeMillis()
        invoiceDao.markCustomerAsPaid(customerName, currentTime)
    }

    suspend fun getAllInvoiceItems(): List<InvoiceItem> {
        return invoiceDao.getAllInvoiceItems()
    }

    fun getAllInvoiceItemsFlow(): Flow<List<InvoiceItem>> {
        return invoiceDao.getAllInvoiceItemsFlow()
    }

    suspend fun clearAllData() {
        productDao.deleteAllProducts()
        invoiceDao.deleteAllInvoices()
        invoiceDao.deleteAllInvoiceItems()
    }

    suspend fun insertInvoiceItemDirectly(item: InvoiceItem) {
        invoiceDao.insertInvoiceItem(item)
    }

    suspend fun insertInvoiceDirectly(invoice: Invoice) {
        invoiceDao.insertInvoice(invoice)
    }
}
