package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.Product
import com.example.ui.components.BarcodeScannerView
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosSaleScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current
    val cart by viewModel.cart.collectAsState()
    val products by viewModel.products.collectAsState()

    var isScannerActive by remember { mutableStateOf(false) }
    var manualBarcode by remember { mutableStateOf("") }
    var showCreditDialog by remember { mutableStateOf(false) }
    var customerName by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    // RTL direction block forced for fully native Arabic alignment
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "شاشة كاشير المبيعات",
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
                // Warning Alert for barcode
                if (statusMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (statusMessage.contains("غير متوفر")) 
                                MaterialTheme.colorScheme.errorContainer 
                            else 
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { statusMessage = "" }
                            )
                        }
                    }
                }

                // 1. Camera Barcode scanner section
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScannerActive) {
                        Column {
                            BarcodeScannerView(
                                onBarcodeScanned = { barcode ->
                                    viewModel.handleScannedBarcode(barcode) { msg ->
                                        statusMessage = msg
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { isScannerActive = false },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إيقاف الكاميرا", color = MaterialTheme.colorScheme.onError)
                            }
                        }
                    } else {
                        Card(
                            onClick = { isScannerActive = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "مسح باركود",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "تشغيل كاميرا مسح الباركود",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "مسح سريع وتلقائي للمنتجات",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Manual barcode input / helper quick items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("أدخل الباركود يدوياً...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Button(
                        onClick = {
                            if (manualBarcode.isNotEmpty()) {
                                viewModel.handleScannedBarcode(manualBarcode) { msg ->
                                    statusMessage = msg
                                }
                                manualBarcode = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text("بحث ومسح", fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "سلة المبيعات الحالية (${cart.size} منتجات)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // 3. Cart list or descriptive empty state
                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "سلة فارغة",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "سلة المبيعات فارغة الآن",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "قم بمسح الباركود بالكاميرا أو أضف منتج يدوي لبدء الحساب",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cart.keys.toList()) { product ->
                            val qty = cart[product] ?: 1
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "كود: ${product.barcode}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = "السعر الفردي: ${product.salePrice} د.ت",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Controls block
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Minus Button
                                        IconButton(
                                            onClick = { viewModel.decrementCartItem(product) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "نقصان",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Text(
                                            text = qty.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.widthIn(min = 20.dp),
                                            textAlign = TextAlign.Center
                                        )

                                        // Plus Button
                                        IconButton(
                                            onClick = { viewModel.incrementCartItem(product) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "زيادة",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Trash Delete Button
                                        IconButton(
                                            onClick = { viewModel.removeFromCart(product) },
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Large Invoice Checkout Footer and Actions
                if (cart.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "المجموع الإجمالي للحساب:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "${viewModel.cartTotal} د.ت",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // COMPLETE SALE (PAID IMMEDIATELY)
                                Button(
                                    onClick = {
                                        viewModel.completeSaleAndCheckout {
                                            Toast.makeText(context, "تم تسجيل العملية كفاتورة مدفوعة بنجاح!", Toast.LENGTH_LONG).show()
                                            statusMessage = "تم الانتهاء بنجاح ومزامنة الربح المكتسب."
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "إنهاء البيع",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                // CREDIT (DEBT ACCOUNT)
                                Button(
                                    onClick = {
                                        customerName = ""
                                        showCreditDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Icon(Icons.Default.HistoryEdu, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "الكريدي (دين)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Customer Name Credit Prompt Dialog
            if (showCreditDialog) {
                var localError by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreditDialog = false },
                    title = {
                        Text(
                            "التسجيل بالدين (الكريدي)",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "يرجى كتابة أو اختيار اسم الزبون لنسجل الفاتورة في ذمته المالية:",
                                fontSize = 14.sp
                            )
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = {
                                    customerName = it
                                    if (it.isNotEmpty()) localError = ""
                                },
                                placeholder = { Text("اسم الزبون (مثال: محمد الباجي)...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (localError.isNotEmpty()) {
                                Text(localError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (customerName.trim().isEmpty()) {
                                    localError = "الرجاء إدخال اسم الزبون لتسجيل الكريدي!"
                                } else {
                                    viewModel.checkoutWithCredit(customerName) {
                                        showCreditDialog = false
                                        Toast.makeText(context, "تم تسجيل الدين لـ $customerName بنجاح!", Toast.LENGTH_LONG).show()
                                        statusMessage = "تم حفظ الكريدي باسم: $customerName"
                                    }
                                }
                            }
                        ) {
                            Text("تأكيد وحفظ")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreditDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }
    }
}
