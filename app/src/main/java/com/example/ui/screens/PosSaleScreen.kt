package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    // Forced RTL Local Direction block for proper and cohesive Arabic/Tunisian POS alignment
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                // Sticky Premium Checkout Container
                AnimatedVisibility(
                    visible = cart.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Total Billing Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        "المجموع الإجمالي للحساب:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    "${viewModel.cartTotal} د.ت",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Action Checkout Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // COMPLETE CASH SALE
                                Button(
                                    onClick = {
                                        viewModel.completeSaleAndCheckout {
                                            Toast.makeText(context, "تم تسجيل العملية كفاتورة مدفوعة بنجاح!", Toast.LENGTH_LONG).show()
                                            statusMessage = "تم الانتهاء بنجاح ومزامنة المبيعات في قاعدة البيانات."
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "إنهاء البيع (نقداً)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
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
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HistoryEdu,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "الكريدي (دين)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Warning Alert for scanner feedback
                if (statusMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (statusMessage.contains("غير متوفر")) 
                                MaterialTheme.colorScheme.errorContainer 
                            else 
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (statusMessage.contains("غير متوفر")) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { statusMessage = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (statusMessage.contains("غير متوفر")) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // 1. Ultra Space-Saving Command Bar (Scanner switch + slim manual barcode field)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Slim manual barcode input field with internal action trigger
                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("أدخل الباركود يدوياً...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (manualBarcode.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        if (manualBarcode.isNotEmpty()) {
                                            viewModel.handleScannedBarcode(manualBarcode) { msg ->
                                                statusMessage = msg
                                            }
                                            manualBarcode = ""
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack, // Points left in RTL (acting as submit arrow)
                                        contentDescription = "بحث",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )

                    // Compact camera toggle button with active/inactive coloring
                    FilledTonalIconButton(
                        onClick = { isScannerActive = !isScannerActive },
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isScannerActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = if (isScannerActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isScannerActive) Icons.Default.VideocamOff else Icons.Default.CameraAlt,
                            contentDescription = "المسح الضوئي",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 2. Expandable Barcode Camera Scanner preview (Only takes vertical height when toggled active)
                AnimatedVisibility(
                    visible = isScannerActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            BarcodeScannerView(
                                onBarcodeScanned = { barcode ->
                                    viewModel.handleScannedBarcode(barcode) { msg ->
                                        statusMessage = msg
                                    }
                                }
                            )
                            // Visual guides overlay to focus scanning target for user
                            Box(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 240.dp, height = 120.dp)
                                        .border(2.dp, Color.Red.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                        .background(Color.Red.copy(alpha = 0.03f))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.65f)
                                        .height(2.dp)
                                        .background(Color.Red.copy(alpha = 0.8f))
                                )
                            }
                            
                            // Top-right close button inside scanner to toggle off instantly
                            IconButton(
                                onClick = { isScannerActive = false },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إلغاء",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "سلة المبيعات الحالية (${cart.size} منتجات)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // 3. Main Cart Items Scroll Area with optimized breathing room
                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "سلة فارغة",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "سلة المبيعات فارغة الآن",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "قم بمسح الباركود بالكاميرا أو أدخل الكود يدوياً وسينتقل المنتج تلقائياً إلى هنا لبدء عملية الحساب السريع.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(cart.keys.toList()) { product ->
                            val qty = cart[product] ?: 1
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // [Right Block] Product Description Info
                                    Column(
                                        modifier = Modifier.weight(1.2f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "السعر الفردي:",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                                            )
                                            Text(
                                                text = "${product.salePrice} د.ت",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // [Center Block] Secondary details (No-fuss thin Barcode font)
                                    Column(
                                        modifier = Modifier.weight(0.8f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "رمز الباركود",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = product.barcode,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                        )
                                    }

                                    // [Left Block] Quantity counter with clear trash container spacing
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        // Unified pill design containing decrement, amount counter, and increment
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                    shape = RoundedCornerShape(24.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                        ) {
                                            // Minus (Decrement) Button
                                            IconButton(
                                                onClick = { viewModel.decrementCartItem(product) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Remove,
                                                    contentDescription = "نقصان",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Text(
                                                text = qty.toString(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.widthIn(min = 28.dp),
                                                textAlign = TextAlign.Center
                                            )

                                            // Plus (Increment) Button
                                            IconButton(
                                                onClick = { viewModel.incrementCartItem(product) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "زيادة",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Accidental-Touch-Protected Red Delete Button
                                        IconButton(
                                            onClick = { viewModel.removeFromCart(product) },
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                                    shape = CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
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
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "يرجى كتابة أو اختيار اسم الزبون لنسجل الفاتورة في ذمته المالية:",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 20.sp
                            )
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = {
                                    customerName = it
                                    if (it.isNotEmpty()) localError = ""
                                },
                                placeholder = { Text("اسم الزبون (مثال: محمد الباجي)...", fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            if (localError.isNotEmpty()) {
                                Text(
                                    text = localError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                            },
                            shape = RoundedCornerShape(12.dp)
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
