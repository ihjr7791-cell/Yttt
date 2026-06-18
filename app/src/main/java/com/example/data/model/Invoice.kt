package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val totalAmount: Double,
    val isPaid: Boolean,           // true = مدفوعة (إنهاء البيع), false = كريدي (ديون بالآجل)
    val customerName: String? = null, // اسم الزبون في حالة الكريدي
    val totalProfit: Double        // مجموع الارباح المحتسبة لهذه الفاتورة
)
