package com.example.todocriss

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController // Added missing import
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.todocriss.ui.theme.ToDoCrissTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToDoCrissTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeScreen(navController) }
                    composable("taskList") { TaskListScreen(navController) }
                    composable("categories") { CategoriesScreen(navController) }
                    composable("categoryTasks/{category}") { backStackEntry ->
                        CategoryTasksScreen(
                            navController,
                            category = backStackEntry.arguments?.getString("category") ?: "All"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    // Animation states
    var showButton by remember { mutableStateOf(false) }
    var showVersion by remember { mutableStateOf(false) }

    // Trigger animations sequentially
    LaunchedEffect(Unit) {
        delay(500)
        showButton = true
        delay(300)
        showVersion = true
    }

    // Enhanced 3D button elevation
    val buttonElevation by animateDpAsState(
        targetValue = if (showButton) 16.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonElevation"
    )

    // Full-screen image background
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Image as background
        Image(
            painter = painterResource(id = R.drawable.branding_guidelines),
            contentDescription = "Branding Guidelines Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        // Button and Version at the bottom
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Enhanced 3D Get Started Button
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
            ) {
                ElevatedButton(
                    onClick = { navController.navigate("taskList") }, // Updated to use NavController
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF536DFE),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = buttonElevation,
                        pressedElevation = 8.dp,
                        hoveredElevation = 20.dp
                    ),
                    modifier = Modifier
                        .height(64.dp)
                        .fillMaxWidth(0.75f)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = Color(0xFF536DFE).copy(alpha = 0.3f),
                            spotColor = Color(0xFF536DFE).copy(alpha = 0.5f)
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF536DFE),
                                    Color(0xFF536DFE)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.3f),
                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Version info at the bottom
            AnimatedVisibility(
                visible = showVersion,
                enter = fadeIn()
            ) {
                Text(
                    text = "Version 1.0",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    )
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
        HomeScreen(navController = rememberNavController()) // Pass mock NavController
    }
}