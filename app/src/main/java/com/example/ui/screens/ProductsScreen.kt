package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.Product
import com.example.ui.components.BarcodeScannerView
import com.example.ui.viewmodel.PosViewModel
import java.io.File
import java.io.FileOutputStream

// Dynamic local helper to save bitmap to file
private fun saveProductImage(context: Context, bitmap: Bitmap): String? {
    return try {
        val file = File(context.filesDir, "product_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current
    val products by viewModel.products.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    var capturedBitmapAdd by remember { mutableStateOf<Bitmap?>(null) }
    var capturedBitmapEdit by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncherAdd = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmapAdd = bitmap
        }
    }

    val cameraLauncherEdit = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmapEdit = bitmap
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "يرجى منح صلاحية الكاميرا لالتقاط صورة للمنتج", Toast.LENGTH_SHORT).show()
        }
    }

    val launchCameraAdd = {
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            cameraLauncherAdd.launch(null)
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val launchCameraEdit = {
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            cameraLauncherEdit.launch(null)
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // RTL block for Arabic
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val filteredProducts = remember(products, searchQuery) {
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.barcode.contains(searchQuery, ignoreCase = true)
            }
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { 
                        capturedBitmapAdd = null
                        showAddDialog = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة منتج")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة منتج", fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث باسم المنتج أو الباركود...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "المنتجات المسجلة (${filteredProducts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Grid of items
                if (filteredProducts.isEmpty()) {
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
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "لا يوجد منتجات",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "لم تقم بإضافة منتجات بعد" else "لا توجد نتائج بحث مطابقة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "اضغط على زر (إضافة منتج) لتعبئة قوائم المبيعات السريعة" else "جرّب البحث بكلمة أو باركود آخر",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProducts) { product ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    // Header with visual tag and Edit / Delete buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "منتج للبيع",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    editingProduct = product
                                                    capturedBitmapEdit = null
                                                    showEditDialog = true
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "تعديل المنتج",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteProductById(product.id)
                                                    Toast.makeText(context, "تم حذف منتج ${product.name}", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف المنتج",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Product icon or captured photo as a modern layout row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!product.imageUrl.isNullOrEmpty()) {
                                            Image(
                                                painter = rememberAsyncImagePainter(
                                                    File(product.imageUrl)
                                                ),
                                                contentDescription = product.name,
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(10.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                        RoundedCornerShape(10.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (product.name.isNotEmpty()) product.name.take(1).uppercase() else "P",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            // Product Name
                                            Text(
                                                text = product.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            // Barcode
                                            Text(
                                                text = "الرمز: ${product.barcode}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Buy & Sell calculations
                                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("الشراء", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                            Text(
                                                "${product.purchasePrice} د.ت",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("البيع", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                "${product.salePrice} د.ت",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Profit representation label
                                    val localProfit = product.salePrice - product.purchasePrice
                                    Text(
                                        text = "الربح الفردي: +${String.format("%.2f", localProfit)} د.ت",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (localProfit > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ADD NEW PRODUCT DIALOG MODAL
            if (showAddDialog) {
                var pBarcode by remember { mutableStateOf("") }
                var pName by remember { mutableStateOf("") }
                var pPurchasePrice by remember { mutableStateOf("") }
                var pSalePrice by remember { mutableStateOf("") }
                var isFormScannerActive by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = {
                        Text(
                            "إضافة منتج بيع سريع",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isFormScannerActive) {
                                BarcodeScannerView(
                                    onBarcodeScanned = { code ->
                                        pBarcode = code
                                        isFormScannerActive = false
                                        viewModel.playBeep()
                                    }
                                )
                                TextButton(
                                    onClick = { isFormScannerActive = false },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("إلغاء وتشغيل إدخال يدوي")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pBarcode,
                                        onValueChange = { pBarcode = it },
                                        placeholder = { Text("كود الباركود (إجباري)") },
                                        label = { Text("الباركود") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    IconButton(
                                        onClick = { isFormScannerActive = true },
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "قراءة باركود المنتج بالكاميرا",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = pName,
                                    onValueChange = { pName = it },
                                    placeholder = { Text("اسم المنتج (مثال: حليب دليس)") },
                                    label = { Text("اسم المنتج") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pPurchasePrice,
                                        onValueChange = { pPurchasePrice = it },
                                        placeholder = { Text("1.25") },
                                        label = { Text("سعر الشراء") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )

                                    OutlinedTextField(
                                        value = pSalePrice,
                                        onValueChange = { pSalePrice = it },
                                        placeholder = { Text("1.50") },
                                        label = { Text("سعر البيع") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "صورة المنتج والرمز التعريفي (اختيارية)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { launchCameraAdd() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (capturedBitmapAdd != null) {
                                            Image(
                                                bitmap = capturedBitmapAdd!!.asImageBitmap(),
                                                contentDescription = "تم التقاط صورة",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Button(
                                            onClick = { launchCameraAdd() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (capturedBitmapAdd != null) "تغيير الصورة" else "التقاط صورة حية",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (capturedBitmapAdd != null) {
                                            TextButton(
                                                onClick = { capturedBitmapAdd = null },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    "إزالة الصورة",
                                                     color = MaterialTheme.colorScheme.error,
                                                     fontSize = 11.sp,
                                                     fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Text(
                                                "اضغط لالتقاط صورة حية كشعار للمنتج",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }

                                if (errorMessage.isNotEmpty()) {
                                    Text(
                                        text = errorMessage,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (!isFormScannerActive) {
                            Button(
                                onClick = {
                                    val curBarcode = pBarcode.trim()
                                    val curName = pName.trim()
                                    val buyPrice = pPurchasePrice.toDoubleOrNull()
                                    val sellPrice = pSalePrice.toDoubleOrNull()
                                    val imgUrl = capturedBitmapAdd?.let { saveProductImage(context, it) }

                                    if (curBarcode.isEmpty() || curName.isEmpty() || buyPrice == null || sellPrice == null) {
                                        errorMessage = "يرجى تعبئة كافة الحقول بشكل صحيح واحتساب الأسعار!"
                                    } else if (sellPrice < buyPrice) {
                                        errorMessage = "تحذير: سعر البيع أقل من سعر الشراء (خسارة)!"
                                    } else {
                                        viewModel.addNewProduct(
                                            curBarcode,
                                            curName,
                                            buyPrice,
                                            sellPrice,
                                            imgUrl
                                        ) { success ->
                                            if (success) {
                                                capturedBitmapAdd = null
                                                showAddDialog = false
                                                Toast.makeText(context, "تم حفظ وتسعير المنتج بنجاح!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("حفظ المنتج")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // EDIT PRODUCT DIALOG MODAL
            if (showEditDialog) {
                var pBarcode by remember { mutableStateOf("") }
                var pName by remember { mutableStateOf("") }
                var pPurchasePrice by remember { mutableStateOf("") }
                var pSalePrice by remember { mutableStateOf("") }
                var pImageUrl by remember { mutableStateOf("") }
                var isFormScannerActive by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf("") }

                LaunchedEffect(editingProduct) {
                    editingProduct?.let {
                        pBarcode = it.barcode
                        pName = it.name
                        pPurchasePrice = it.purchasePrice.toString()
                        pSalePrice = it.salePrice.toString()
                        pImageUrl = it.imageUrl ?: ""
                    }
                }

                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = {
                        Text(
                            "تعديل بيانات المنتج",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isFormScannerActive) {
                                BarcodeScannerView(
                                    onBarcodeScanned = { code ->
                                        pBarcode = code
                                        isFormScannerActive = false
                                        viewModel.playBeep()
                                    }
                                )
                                TextButton(
                                    onClick = { isFormScannerActive = false },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("إلغاء وتشغيل إدخال يدوي")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pBarcode,
                                        onValueChange = { pBarcode = it },
                                        placeholder = { Text("كود الباركود (إجباري)") },
                                        label = { Text("الباركود") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    IconButton(
                                        onClick = { isFormScannerActive = true },
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "قراءة باركود المنتج بالكاميرا",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = pName,
                                    onValueChange = { pName = it },
                                    placeholder = { Text("اسم المنتج") },
                                    label = { Text("اسم المنتج") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pPurchasePrice,
                                        onValueChange = { pPurchasePrice = it },
                                        placeholder = { Text("سعر الشراء") },
                                        label = { Text("سعر الشراء") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )

                                    OutlinedTextField(
                                        value = pSalePrice,
                                        onValueChange = { pSalePrice = it },
                                        placeholder = { Text("سعر البيع") },
                                        label = { Text("سعر البيع") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "صورة المنتج والرمز التعريفي (اختيارية)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { launchCameraEdit() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (capturedBitmapEdit != null) {
                                            Image(
                                                bitmap = capturedBitmapEdit!!.asImageBitmap(),
                                                contentDescription = "تم التقاط صورة",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else if (pImageUrl.isNotEmpty()) {
                                            Image(
                                                painter = rememberAsyncImagePainter(
                                                    File(pImageUrl)
                                                ),
                                                contentDescription = "الصورة الحالية",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Button(
                                            onClick = { launchCameraEdit() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (capturedBitmapEdit != null || pImageUrl.isNotEmpty()) "تغيير الصورة" else "التقاط صورة حية",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (capturedBitmapEdit != null || pImageUrl.isNotEmpty()) {
                                            TextButton(
                                                onClick = { 
                                                    capturedBitmapEdit = null
                                                    pImageUrl = ""
                                                },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    "إزالة الصورة",
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Text(
                                                "اضغط لالتقاط صورة حية كشعار للمنتج",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }

                                if (errorMessage.isNotEmpty()) {
                                    Text(
                                        text = errorMessage,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (!isFormScannerActive) {
                            Button(
                                onClick = {
                                    val curBarcode = pBarcode.trim()
                                    val curName = pName.trim()
                                    val buyPrice = pPurchasePrice.toDoubleOrNull()
                                    val sellPrice = pSalePrice.toDoubleOrNull()
                                    val imgUrl = if (capturedBitmapEdit != null) {
                                        saveProductImage(context, capturedBitmapEdit!!)
                                    } else {
                                        if (pImageUrl.isEmpty()) null else pImageUrl
                                    }

                                    if (curBarcode.isEmpty() || curName.isEmpty() || buyPrice == null || sellPrice == null) {
                                        errorMessage = "يرجى تعبئة كافة الحقول بشكل صحيح واحتساب الأسعار!"
                                    } else if (sellPrice < buyPrice) {
                                        errorMessage = "تحذير: سعر البيع أقل من سعر الشراء (خسارة)!"
                                    } else {
                                        editingProduct?.let { original ->
                                            val updated = original.copy(
                                                barcode = curBarcode,
                                                name = curName,
                                                purchasePrice = buyPrice,
                                                salePrice = sellPrice,
                                                imageUrl = imgUrl
                                            )
                                            viewModel.updateProduct(updated) { success ->
                                                if (success) {
                                                    capturedBitmapEdit = null
                                                    showEditDialog = false
                                                    Toast.makeText(context, "تم تحديث المنتج بنجاح!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("حفظ التعديلات")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }
    }
}
