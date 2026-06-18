package com.example.data.local

import androidx.room.*
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItem(item: InvoiceItem)

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: Int): List<InvoiceItem>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getInvoiceItemsFlow(invoiceId: Int): Flow<List<InvoiceItem>>

    @Query("UPDATE invoices SET isPaid = 1, timestamp = :timestamp WHERE customerName = :customerName AND isPaid = 0")
    suspend fun markCustomerAsPaid(customerName: String, timestamp: Long)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceItemsByInvoiceId(invoiceId: Int)
}
