package com.example.todocriss

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todocriss.ui.theme.ToDoCrissTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToDoCrissTheme {
                HomeScreen(
                    onNavigateToList = {
                        val intent = Intent(this, ListActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun HomeScreen(onNavigateToList: () -> Unit, modifier: Modifier = Modifier) {
    // Animation states
    var showTitle by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    // Trigger animations sequentially
    LaunchedEffect(Unit) {
        delay(300)
        showTitle = true
        delay(500)
        showSubtitle = true
        delay(700)
        showButton = true
    }

    // Animated elevation for the button
    val buttonElevation by animateDpAsState(
        targetValue = if (showButton) 8.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonElevation"
    )

    // Gradient background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F7FF),
                        Color(0xFFEAEEFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo or icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6200EA).copy(alpha = 0.1f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Replace R.drawable.ic_todo with your actual checklist icon
                // Using a placeholder vector for now
                Text(
                    text = "✓",
                    fontSize = 48.sp,
                    color = Color(0xFF6200EA),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome Message with animation
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
            ) {
                Text(
                    text = "Welcome to",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center
                )
            }

            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
            ) {
                Text(
                    text = "Criss's TO DO List",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF6200EA),
                                Color(0xFF9D50BB)
                            )
                        ),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
            }

            // Card for developer info
            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DEVELOPED BY",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "Criss",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Text(
                            text = "D/BCS/23/0015",
                            fontSize = 16.sp,
                            color = Color(0xFF6200EA),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Go to Lists Button with animation
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
            ) {
                ElevatedButton(
                    onClick = onNavigateToList,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF6200EA),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = buttonElevation,
                        pressedElevation = 4.dp
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.8f)
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Version info at the bottom
            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn()
            ) {
                Text(
                    text = "Version 1.0",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Suppress("Unused")
@Composable
@Preview(showBackground = true)
fun HomeScreenPreview() {
    ToDoCrissTheme {
        HomeScreen(onNavigateToList = {})
    }
}