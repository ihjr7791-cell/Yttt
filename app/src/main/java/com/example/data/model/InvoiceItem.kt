package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceId: Int, // رقم الفاتورة المرتبطة بها
    val productBarcode: String,
    val productName: String,
    val purchasePrice: Double,
    val salePrice: Double,
    val quantity: Int
)
