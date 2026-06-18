package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.PosRepository
import com.example.ui.screens.MainAppContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CosmicSlateBackground
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup direct SQL abstraction layers
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PosRepository(database)
        
        // Viewmodel Provider Factory instantiation
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PosViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return PosViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        val viewModel = ViewModelProvider(this, factory)[PosViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var showSplashScreen by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1600) // Beautiful 1.6s cashier welcome splash
                    showSplashScreen = false
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = showSplashScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "SplashScreenAnimation"
                    ) { activeSplash ->
                        if (activeSplash) {
                            ArabicSplashScreen()
                        } else {
                            MainAppContainer(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArabicSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicSlateBackground), // Charcoal Slate Theme background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Shining cash register icon container
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFF00A86B).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PointOfSale,
                    contentDescription = null,
                    tint = Color(0xFF00A86B), // Beautiful Emerald Green
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "نظام الكاشير الذكي",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "نقاط بيع وتسعير وإحصائيات أرباح متكاملة",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color(0xFF00A86B),
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "إعداد قواعد البيانات المحلية...",
                color = Color(0xFF94A3B8).copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}
