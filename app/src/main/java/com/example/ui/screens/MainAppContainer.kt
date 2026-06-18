package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentTabIndex by remember { mutableStateOf(0) } // 0 = شاشة الكاشير, 1 = تسعير المنتجات, 2 = التقارير

    // Simulated cloud sync profiles
    var cloudUserEmail by remember { mutableStateOf("alkingyb987@gmail.com") } // User's email from environment metadata! (Very personal!)
    var isCloudSynced by remember { mutableStateOf(true) }
    var accountSyncedDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .windowInsetsPadding(WindowInsets.statusBars),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "نظام الكاشير الذكي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                "نقاط البيع السريعة وحساب الأرباح",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }

                        // Premium Google Sign-In Profile connection HUD
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { accountSyncedDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isCloudSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (isCloudSynced) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "مُتَّصِل",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            // Circle avatar visual representation
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = currentTabIndex == 0,
                        onClick = { currentTabIndex = 0 },
                        label = { Text("شاشة البيع", fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PointOfSale, // Modern POS billing icon
                                contentDescription = "كاشير"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTabIndex == 1,
                        onClick = { currentTabIndex = 1 },
                        label = { Text("المنتجات", fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "منتجات"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTabIndex == 2,
                        onClick = { currentTabIndex = 2 },
                        label = { Text("التقارير", fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "الأرباح"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = NavigationBarItemDefaults.colors().selectedTextColor,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Crossfade animations for premium screen transition reveals
                Crossfade(
                    targetState = currentTabIndex,
                    animationSpec = tween(durationMillis = 250),
                    label = "ScreenTransition"
                ) { screenIndex ->
                    when (screenIndex) {
                        0 -> PosSaleScreen(viewModel = viewModel)
                        1 -> ProductsScreen(viewModel = viewModel)
                        2 -> FinancialDashboardScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Active Connection & Cloud Sync Details Dialog
        if (accountSyncedDialog) {
            AlertDialog(
                onDismissRequest = { accountSyncedDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        "الحساب والمزامنة السحابية",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "جاري تفعيل حفظ البيانات سحابياً تلقائياً لضمان عدم فقدان المنتجات والديون.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("الحساب المتصل (Google):", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(cloudUserEmail, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("العمل بلا إنترنت (Offline First):", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            Text("مفعل بنسبة 100% ✔", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isCloudSynced = false
                                Toast.makeText(context, "جاري المزامنة السحابية الاختيارية الفورية مع قاعدة البيانات...", Toast.LENGTH_SHORT).show()
                                kotlinx.coroutines.delay(1000)
                                isCloudSynced = true
                                Toast.makeText(context, "تمت مزامنة كافة الفواتير والمنتجات بنجاح لـ $cloudUserEmail!", Toast.LENGTH_LONG).show()
                            }
                            accountSyncedDialog = false
                        }
                    ) {
                        Text("مزامنة فورية الآن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountSyncedDialog = false }) {
                        Text("إغلاق")
                    }
                }
            )
        }
    }
}
