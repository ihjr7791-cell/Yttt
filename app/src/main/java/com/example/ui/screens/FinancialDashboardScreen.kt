package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
    var selectedDebtorName by remember { mutableStateOf<String?>(null) }
    var showPartialPayDialog by remember { mutableStateOf(false) }
    var partialPayAmountInput by remember { mutableStateOf("") }
    var showAddNewDebtDialog by remember { mutableStateOf(false) }
    var newDebtAmountInput by remember { mutableStateOf("") }
    var newDebtDetailsInput by remember { mutableStateOf("") }

    val monthFormatter = remember { SimpleDateFormat("yyyy/MM", Locale("ar")) }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")) }

    // RTL Block
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        
        // Calculations
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
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Selector Tabs (Reports, Logs, Credit List)
                TabRow(
                    selectedTabIndex = activeTabState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = activeTabState == 0,
                        onClick = { activeTabState = 0 },
                        text = { Text("الأرباح والتقارير", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = activeTabState == 1,
                        onClick = { activeTabState = 1 },
                        text = { Text("سجل الفواتير", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = activeTabState == 2,
                        onClick = { activeTabState = 2 },
                        text = { Text("الكريدي والديون", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                when (activeTabState) {
                    0 -> { // Dashboard general stats overview
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                Text(
                                    "الحالة المالية ومؤشرات الأداء",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
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
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("إجمالي المبيعات المستخلصة (نقداً)", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 13.sp)
                                        Text(
                                            "${String.format("%.2f", totalAllRevenues)} د.ت",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 28.sp
                                        )
                                    }
                                }
                            }

                            // 2. Paid Profits and Debts Cards row
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("صافي الأرباح", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "${String.format("%.2f", totalAllProfits)} د.ت",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("الديون المعلقة (كريدي)", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "${String.format("%.2f", totalAllDebts)} د.ت",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            item {
                                Text(
                                    "تقرير المبيعات والربح الشهري",
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
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "لا توجد أي فواتير مدفوعة في التاريخ حتى الآن لمقارنتها.",
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
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
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
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        "دورة الشهر: ${report.month}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        "حجم المبيعات: ${String.format("%.2f", report.revenue)} د.ت",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }

                                            Text(
                                                "الأرباح: +${String.format("%.2f", report.profit)} د.ت",
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

                    1 -> { // Invoice history logs
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
                                    Text("سجل الفواتير فارغ تماماً حالياً", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
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
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val statusLabel = if (invoice.isPaid) "مدفوعة" else "كريدي معلق"
                                                    val statusColor = if (invoice.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                statusColor.copy(alpha = 0.12f),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
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
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.tertiary
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "التاريخ: ${dateFormatter.format(Date(invoice.timestamp))}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )

                                                Text(
                                                    text = "المبلغ: ${invoice.totalAmount} د.ت  |  صافي المربح: ${String.format("%.2f", invoice.totalProfit)} د.ت",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteInvoice(invoice.id)
                                                    Toast.makeText(context, "تم إلغاء وحذف الفاتورة بنجاح", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف الفاتورة",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // Debtors management with user inputs (CRITICAL IMPROVEMENT)
                        var manualNameInput by remember { mutableStateOf("") }
                        var manualDebtAmount by remember { mutableStateOf("") }
                        val invoiceItems by viewModel.invoiceItems.collectAsState()

                        if (selectedDebtorName != null) {
                            val debtorName = selectedDebtorName!!
                            val customerInvoices = remember(invoices, debtorName) {
                                invoices.filter { !it.isPaid && it.customerName?.equals(debtorName, ignoreCase = true) == true }
                                    .sortedByDescending { it.timestamp }
                            }
                            val debtorTotalDebt = customerInvoices.sumOf { it.totalAmount }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Header & Back Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { selectedDebtorName = null }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "رجوع بقائمة الديون"
                                        )
                                    }
                                    Text(
                                        text = "كشف حساب: $debtorName",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Total Debt Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "إجمالي الديون المتراكمة",
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "${String.format("%.2f", debtorTotalDebt)} د.ت",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 28.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Settle all debt
                                            Button(
                                                onClick = {
                                                    viewModel.settleCustomerDebts(debtorName)
                                                    selectedDebtorName = null
                                                    Toast.makeText(context, "تم سداد حساب $debtorName بالكامل بنجاح!", Toast.LENGTH_LONG).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.PriceCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("سداد كلي (تصفير)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Partial Payment
                                            OutlinedButton(
                                                onClick = {
                                                    partialPayAmountInput = ""
                                                    showPartialPayDialog = true
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("سداد جزئي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                newDebtAmountInput = ""
                                                newDebtDetailsInput = ""
                                                showAddNewDebtDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تسجيل كريدي جديد للزبون", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Text(
                                    "الحركة التفصيلية لفواتير الكريدي (${customerInvoices.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (customerInvoices.isEmpty()) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("سجل الديون فارغ أو تم سداده بالكامل.", color = MaterialTheme.colorScheme.outline)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(bottom = 8.dp)
                                    ) {
                                        items(customerInvoices) { invoice ->
                                            val invoiceProducts = remember(invoiceItems, invoice.id) {
                                                invoiceItems.filter { it.invoiceId == invoice.id }
                                            }

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = dateFormatter.format(Date(invoice.timestamp)),
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.outline,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = "فاتورة كريدي #${invoice.id}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.tertiary
                                                        )
                                                    }

                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                                    // Products listed here
                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        if (invoiceProducts.isEmpty()) {
                                                            Text(
                                                                "• دين يدوي (دون تفصيل سلة)",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.outline
                                                            )
                                                        } else {
                                                            invoiceProducts.forEach { item ->
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                                ) {
                                                                    Text(
                                                                        "• ${item.productName}",
                                                                        fontSize = 13.sp,
                                                                        fontWeight = FontWeight.Medium
                                                                    )
                                                                    Text(
                                                                        "${item.quantity} × ${String.format("%.2f", item.salePrice)} د.ت",
                                                                        fontSize = 12.sp,
                                                                        color = MaterialTheme.colorScheme.outline
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            "المبلغ المستحق",
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                        Text(
                                                            "${String.format("%.2f", invoice.totalAmount)} د.ت",
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 16.sp,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. Sleek, polished top-form card to record custom debt values directly (User Intent)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddCard,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                "إضافة أو تسجيل دين خارجي يدوياً",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = manualNameInput,
                                                onValueChange = { manualNameInput = it },
                                                placeholder = { Text("اسم الشخص المستدين...", fontSize = 12.sp) },
                                                modifier = Modifier.weight(1.3f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                                )
                                            )

                                            OutlinedTextField(
                                                value = manualDebtAmount,
                                                onValueChange = { manualDebtAmount = it },
                                                placeholder = { Text("المبلغ (د.ت)...", fontSize = 12.sp) },
                                                modifier = Modifier.weight(0.9f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                                )
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val amt = manualDebtAmount.toDoubleOrNull() ?: 0.0
                                                if (manualNameInput.trim().isEmpty() || amt <= 0.0) {
                                                    Toast.makeText(context, "الرجاء كتابة اسم مستدين جملي ومبلغ حقيقي صالح!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.addManualDebt(manualNameInput, amt) { ok ->
                                                        if (ok) {
                                                            Toast.makeText(context, "تم تسجيل دين بقيمة $amt لـ $manualNameInput يدوياً!", Toast.LENGTH_LONG).show()
                                                            manualNameInput = ""
                                                            manualDebtAmount = ""
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("حفظ وتسجيل الدين بالملف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // 2. Debts and Credit clients listing
                                Text(
                                    "جميع الأشخاص المدينين بالكريدي (${customerDebts.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (customerDebts.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                "ممتاز! الذمم المالية سليمة وخالية من الكريدي الديون متخلدة بالذمة.",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.outline,
                                                textAlign = TextAlign.Center,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(bottom = 8.dp)
                                    ) {
                                        items(customerDebts) { debtor ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedDebtorName = debtor.name },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
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
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Person,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                        Column {
                                                            Text(
                                                                debtor.name,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp
                                                            )
                                                            Text(
                                                                "المتخلد بالذمة: ${String.format("%.2f", debtor.totalAmount)} د.ت",
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.error,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }

                                                    // View History Details indicator
                                                    Icon(
                                                        imageVector = Icons.Default.ChevronLeft,
                                                        contentDescription = "عرض كشف الحساب التابع له",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
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

            // PARTIAL DEBT PAYMENT DIALOG
            if (showPartialPayDialog && selectedDebtorName != null) {
                val debtorName = selectedDebtorName!!
                AlertDialog(
                    onDismissRequest = { showPartialPayDialog = false },
                    title = {
                        Text(
                            "سداد جزئي للدين",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "الرجاء تحديد قيمة المبلغ الجزئي الذي سدده الزبون [$debtorName] نقداً لتنزيله من حسابه المفتوح:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            OutlinedTextField(
                                value = partialPayAmountInput,
                                onValueChange = { partialPayAmountInput = it },
                                placeholder = { Text("مثال: 15.500", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amt = partialPayAmountInput.toDoubleOrNull() ?: 0.0
                                if (amt <= 0.0) {
                                    Toast.makeText(context, "يرجى كتابة مبلغ حقيقي صالح!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.payPartialDebt(debtorName, amt) { ok, msg ->
                                        if (ok) {
                                            showPartialPayDialog = false
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("تأكيد السداد")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPartialPayDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // ADD NEW DEBT DIALOG
            if (showAddNewDebtDialog && selectedDebtorName != null) {
                val debtorName = selectedDebtorName!!
                AlertDialog(
                    onDismissRequest = { showAddNewDebtDialog = false },
                    title = {
                        Text(
                            "تسجيل كريدي جديد",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "يرجى تحديد تفاصيل الدين وقيمته لتسجيل فاتورة كريدي إضافية للزبون [$debtorName]:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            OutlinedTextField(
                                value = newDebtAmountInput,
                                onValueChange = { newDebtAmountInput = it },
                                placeholder = { Text("أدخل مبلغ الدين (مثال: 25.500)", fontSize = 13.sp) },
                                label = { Text("قيمة الدين (د.ت)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = newDebtDetailsInput,
                                onValueChange = { newDebtDetailsInput = it },
                                placeholder = { Text("تفاصيل اختيارية أو أسماء المنتجات...", fontSize = 13.sp) },
                                label = { Text("التفاصيل / البيان") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amt = newDebtAmountInput.toDoubleOrNull() ?: 0.0
                                if (amt <= 0.0) {
                                    Toast.makeText(context, "الرجاء كتابة مبلغ صحيح أكبر من الصفر!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addManualDebtWithDetails(debtorName, amt, newDebtDetailsInput) { success ->
                                        if (success) {
                                            showAddNewDebtDialog = false
                                            Toast.makeText(context, "تم تسجيل دين جديد بقيمة $amt د.ت لـ $debtorName بنجاح!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "فشل تسجيل الدين. الرجاء المحاولة مجدداً.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("تأكيد التسجيل")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddNewDebtDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
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
