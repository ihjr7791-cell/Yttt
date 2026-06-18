package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDashboardScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current
    val invoices by viewModel.invoices.collectAsState()

    var activeTabState by remember { mutableStateOf(0) } // 0 = لوحة التحكم والتقارير, 1 = سجل الفواتير, 2 = قائمة الديون والكريدي

    val monthFormatter = remember { SimpleDateFormat("yyyy/MM", Locale("ar")) }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")) }

    // RTL Block
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        
        // 1. Calculations
        // Group paid invoices by month
        val paidInvoices = invoices.filter { it.isPaid }
        val unpaidInvoices = invoices.filter { !it.isPaid }

        val monthlyReports = remember(paidInvoices) {
            paidInvoices.groupBy { monthFormatter.format(Date(it.timestamp)) }
                .map { (month, list) ->
                    val totalRevenue = list.sumOf { it.totalAmount }
                    val totalProfit = list.sumOf { it.totalProfit }
                    MonthlyData(month = month, revenue = totalRevenue, profit = totalProfit)
                }
                .sortedByDescending { it.month }
        }

        // Debts calculations
        val customerDebts = remember(unpaidInvoices) {
            unpaidInvoices.groupBy { it.customerName ?: "زبون مجهول" }
                .map { (name, list) ->
                    val totalDebt = list.sumOf { it.totalAmount }
                    CustomerDebt(name = name, totalAmount = totalDebt)
                }
                .sortedByDescending { it.totalAmount }
        }

        val totalAllRevenues = paidInvoices.sumOf { it.totalAmount }
        val totalAllProfits = paidInvoices.sumOf { it.totalProfit }
        val totalAllDebts = unpaidInvoices.sumOf { it.totalAmount }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "التقارير المالية والديون",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Main visual navigation tabs selector
                TabRow(
                    selectedTabIndex = activeTabState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTabState == 0,
                        onClick = { activeTabState = 0 },
                        text = { Text("الأرباح والتقارير", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTabState == 1,
                        onClick = { activeTabState = 1 },
                        text = { Text("سجل الفواتير", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTabState == 2,
                        onClick = { activeTabState = 2 },
                        text = { Text("قائمة الكريدي", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Contacts, contentDescription = null) }
                    )
                }

                // Render dynamic views based on activeTabState
                when (activeTabState) {
                    0 -> { // Dashboard main overview
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                Text(
                                    "الحالة المالية العامة للتطبيق",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // 1. Paid Revenues Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("إجمالي المداخيل الجملية المدفوعة", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 13.sp)
                                        Text(
                                            "${String.format("%.2f", totalAllRevenues)} د.ت",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 28.sp
                                        )
                                    }
                                }
                            }

                            // 2. Paid Profits and Debts Cards
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("صافي المرابيح", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "${String.format("%.2f", totalAllProfits)} د.ت",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("إجمالي الديون المعلقة", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "${String.format("%.2f", totalAllDebts)} د.ت",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Divider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            item {
                                Text(
                                    "التقرير والفرز الشهري للمبيعات والربح",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (monthlyReports.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "لا توجد عمليات مبيعات مدفوعة مسجلة للاحتساب هذا الشهر.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(monthlyReports) { report ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Column {
                                                    Text(
                                                        "الشهر: ${report.month}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        "المداخيل: ${String.format("%.2f", report.revenue)} د.ت",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }

                                            Text(
                                                "الربح: +${String.format("%.2f", report.profit)} د.ت",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // Invoice history list view
                        if (invoices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("سجل الفواتير فارغ حالياً", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(invoices) { invoice ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    val statusLabel = if (invoice.isPaid) "مدفوعة" else "آجل (كريدي)"
                                                    val statusColor = if (invoice.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                statusColor.copy(alpha = 0.15f),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = statusLabel,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = statusColor
                                                        )
                                                    }

                                                    if (!invoice.customerName.isNullOrEmpty()) {
                                                        Text(
                                                            "للزبون: ${invoice.customerName}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.tertiary
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = "تاريخ: ${dateFormatter.format(Date(invoice.timestamp))}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )

                                                Text(
                                                    text = "القيمة: ${invoice.totalAmount} د.ت  |  الأرباح: ${String.format("%.2f", invoice.totalProfit)} د.ت",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            // Delete Button (Updates dynamic reports immediately)
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteInvoice(invoice.id)
                                                    Toast.makeText(context, "تم إلغاء الفاتورة وتصحيح الحسابات الجملية", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "مسح الفاتورة",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // Credit debtors list view
                        if (customerDebts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "ممتاز! لا توجد ديون كريدي معلقة حالياً.",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(customerDebts) { debtor ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary
                                                )
                                                Column {
                                                    Text(
                                                        debtor.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    )
                                                    Text(
                                                        "الدين غير المستخلص: ${String.format("%.2f", debtor.totalAmount)} د.ت",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            // Pay Back / settle debts Button
                                            Button(
                                                onClick = {
                                                    viewModel.settleCustomerDebts(debtor.name)
                                                    Toast.makeText(context, "تم سداد كافة ديون ${debtor.name} وجدولتها في الأرباح!", Toast.LENGTH_LONG).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.PriceCheck, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("تم السداد", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Data holder modeling
data class MonthlyData(
    val month: String,
    val revenue: Double,
    val profit: Double
)

data class CustomerDebt(
    val name: String,
    val totalAmount: Double
)
