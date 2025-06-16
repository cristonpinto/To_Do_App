package com.example.todocriss






import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Define beautiful gradient colors
object AppColors {
    // Primary color palette - modern blue with teal undertones
    val PrimaryBlue = Color(0xFF3B82F6)
    val SecondaryBlue = Color(0xFF60A5FA)
    val AccentBlue = Color(0xFF2563EB)

    // Background colors - subtle light tones
    val LightBackground = Color(0xFFF8FAFC)
    val CardBackground = Color(0xFFFFFFFF)

    // Text colors - high contrast for readability
    val TextPrimary = Color(0xFF1E293B)
    val TextSecondary = Color(0xFF64748B)

    // Status colors - vibrant but not harsh
    val SuccessGreen = Color(0xFF10B981)
    val WarningOrange = Color(0xFFF59E0B)
    val ErrorRed = Color(0xFFEF4444)
    val InfoBlue = Color(0xFF0EA5E9)

    // Accent colors for categories
    val AccentPurple = Color(0xFF8B5CF6)
    val AccentPink = Color(0xFFEC4899)
    val AccentTeal = Color(0xFF14B8A6)
    val AccentIndigo = Color(0xFF6366F1)

    // Gradients
    val GradientBrush = Brush.linearGradient(
        colors = listOf(PrimaryBlue, AccentIndigo)
    )

    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(LightBackground, Color(0xFFF1F5F9))
    )

    val SuccessGradient = Brush.linearGradient(
        colors = listOf(SuccessGreen, Color(0xFF34D399))
    )
}

// Category data class with icon and color
data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val taskCount: Int = 0,
    val isDefault: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(navController: NavController) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val taskDao = db.taskDao()

    // Default categories with enhanced colors
    val defaultCategories = listOf(
        CategoryItem("Personal", Icons.Rounded.Person, AppColors.SuccessGreen, 8, true),
        CategoryItem("Work", Icons.Rounded.Work, AppColors.AccentBlue, 12, true),
        CategoryItem("Shopping", Icons.Rounded.ShoppingCart, AppColors.WarningOrange, 6, true),
        CategoryItem("Health", Icons.Rounded.Favorite, AppColors.ErrorRed, 4, true)
    )

    var categories by remember { mutableStateOf(mutableStateListOf<CategoryItem>()) }
    var newCategory by remember { mutableStateOf("") }
    var isAddingCategory by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val firebaseDb = Firebase.database

    // Scroll state animation
    val scrollState = remember { Animatable(0f) }

    // Function to get icon for category - using Rounded icons for more modern look
    fun getCategoryIcon(categoryName: String): ImageVector {
        return when (categoryName.lowercase()) {
            "travel", "traveling" -> Icons.Rounded.Flight
            "food" -> Icons.Rounded.Restaurant
            "fitness" -> Icons.Rounded.FitnessCenter
            "study", "education" -> Icons.Rounded.School
            "finance", "money" -> Icons.Rounded.AttachMoney
            "entertainment" -> Icons.Rounded.MovieFilter
            "family" -> Icons.Rounded.People
            "business" -> Icons.Rounded.Business
            "home" -> Icons.Rounded.Home
            "technology" -> Icons.Rounded.Devices
            "hobby" -> Icons.Rounded.Palette
            "social" -> Icons.Rounded.Groups
            else -> Icons.Rounded.Label
        }
    }

    // Function to get color for category - enhanced color palette
    fun getCategoryColor(categoryName: String): Color {
        return when (categoryName.lowercase()) {
            "travel", "traveling" -> Color(0xFF0EA5E9) // Sky blue
            "food" -> Color(0xFFF97316) // Orange
            "fitness" -> Color(0xFF22C55E) // Green
            "study", "education" -> AppColors.AccentPurple
            "finance", "money" -> Color(0xFFEAB308) // Yellow
            "entertainment" -> AppColors.AccentPink
            "family" -> AppColors.AccentTeal
            "business" -> Color(0xFF334155) // Slate
            "home" -> Color(0xFF8B5CF6) // Purple
            "technology" -> Color(0xFF3B82F6) // Blue
            "hobby" -> Color(0xFFEC4899) // Pink
            "social" -> Color(0xFF6366F1) // Indigo
            else -> AppColors.PrimaryBlue
        }
    }

    // Replace the entire LaunchedEffect block with this corrected version
    LaunchedEffect(Unit) {
        val categoriesRef = firebaseDb.getReference("categories")
        val tasksRef = firebaseDb.getReference("tasks")

        // Combined listener for both categories and tasks
        val updateCategories = {
            categoriesRef.get().addOnSuccessListener { categoriesSnapshot ->
                tasksRef.get().addOnSuccessListener { tasksSnapshot ->
                    val firebaseCategories = mutableListOf<CategoryItem>()

                    // Add default categories with actual task counts
                    val defaultCategoriesWithCounts = defaultCategories.map { defaultCat ->
                        val taskCount = tasksSnapshot.child(defaultCat.name).children.count()
                        defaultCat.copy(taskCount = taskCount)
                    }
                    firebaseCategories.addAll(defaultCategoriesWithCounts)

                    // Add custom categories from Firebase categories node
                    for (categorySnapshot in categoriesSnapshot.children) {
                        val categoryName = categorySnapshot.key
                        if (categoryName != null &&
                            !defaultCategories.any { it.name.equals(categoryName, ignoreCase = true) }) {

                            // Get task count from tasks node
                            val taskCount = tasksSnapshot.child(categoryName).children.count()

                            firebaseCategories.add(
                                CategoryItem(
                                    name = categoryName,
                                    icon = getCategoryIcon(categoryName),
                                    color = getCategoryColor(categoryName),
                                    taskCount = taskCount,
                                    isDefault = false
                                )
                            )
                        }
                    }

                    categories.clear()
                    categories.addAll(firebaseCategories)
                    isLoading = false

                    Log.d("Firebase", "Loaded ${categories.size} categories with updated task counts")
                }.addOnFailureListener { error ->
                    Log.e("Firebase", "Failed to load tasks: ${error.message}")
                    // Still show categories even if tasks fail to load
                    val firebaseCategories = mutableListOf<CategoryItem>()
                    firebaseCategories.addAll(defaultCategories)

                    for (categorySnapshot in categoriesSnapshot.children) {
                        val categoryName = categorySnapshot.key
                        if (categoryName != null &&
                            !defaultCategories.any { it.name.equals(categoryName, ignoreCase = true) }) {

                            firebaseCategories.add(
                                CategoryItem(
                                    name = categoryName,
                                    icon = getCategoryIcon(categoryName),
                                    color = getCategoryColor(categoryName),
                                    taskCount = 0,
                                    isDefault = false
                                )
                            )
                        }
                    }

                    categories.clear()
                    categories.addAll(firebaseCategories)
                    isLoading = false
                }
            }.addOnFailureListener { error ->
                Log.e("Firebase", "Failed to load categories: ${error.message}")
                categories.clear()
                categories.addAll(defaultCategories)
                isLoading = false
            }
        }

        // Listen to categories changes
        categoriesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateCategories()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Categories listener cancelled: ${error.message}")
                categories.clear()
                categories.addAll(defaultCategories)
                isLoading = false
            }
        })

        // Listen to tasks changes to update counts
        tasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateCategories()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Tasks listener cancelled: ${error.message}")
            }
        })
    }

    // Animation states
    val headerScale by animateFloatAsState(
        targetValue = if (showSuccessAnimation) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "headerScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.BackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Modern Header with Gradient and Elevated Design
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(headerScale)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = AppColors.PrimaryBlue.copy(alpha = 0.2f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.GradientBrush)
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Categories",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Organize your tasks beautifully",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                letterSpacing = 0.2.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Category,
                                contentDescription = "Categories",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Row - Modern UI Element
            if (!isLoading && categories.isNotEmpty()) {


                Spacer(modifier = Modifier.height(16.dp))
            }

            // Loading indicator with modern design
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AppColors.CardBackground
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = AppColors.PrimaryBlue,
                                        modifier = Modifier.size(48.dp),
                                        strokeWidth = 4.dp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Loading categories...",
                                        color = AppColors.TextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Categories List with Beautiful Cards - Enhanced design
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(categories) { index, category ->
                        var isPressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.97f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "categoryScale"
                        )

                        val elevation by animateFloatAsState(
                            targetValue = if (isPressed) 2f else 8f,
                            label = "cardElevation"
                        )

                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(
                                animationSpec = tween(300, delayMillis = index * 50)
                            ) + fadeIn(animationSpec = tween(300, delayMillis = index * 50))
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .shadow(
                                        elevation = elevation.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = category.color.copy(alpha = 0.15f)
                                    )
                                    .clickable {
                                        isPressed = true
                                        navController.navigate("categoryTasks/${category.name}")
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = AppColors.CardBackground
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Modern Category Icon with Gradient Background
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            category.color,
                                                            category.color.copy(alpha = 0.7f)
                                                        )
                                                    ),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = category.icon,
                                                contentDescription = category.name,
                                                tint = Color.White,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = category.name,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AppColors.TextPrimary,
                                                    letterSpacing = (-0.3).sp
                                                )

                                                // Badge for default categories - modern pill design
                                                if (category.isDefault) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = AppColors.SuccessGreen.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "Default",
                                                            fontSize = 10.sp,
                                                            color = AppColors.SuccessGreen,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Task count with subtle visual indicator
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = "Tasks",
                                                    tint = AppColors.TextSecondary.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (category.taskCount == 1) "1 task" else "${category.taskCount} tasks",
                                                    fontSize = 13.sp,
                                                    color = AppColors.TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    // Modern Arrow Button
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(category.color.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowForward,
                                            contentDescription = "Go to category",
                                            tint = category.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Reset pressed state after animation
                        LaunchedEffect(isPressed) {
                            if (isPressed) {
                                delay(100)
                                isPressed = false
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Buttons - Enhanced modern design
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add Category Button - Modern Elevated FAB (Updated to trigger modal)
                FloatingActionButton(
                    onClick = {
                        isAddingCategory = true // This will now trigger the modal bottom sheet
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = AppColors.PrimaryBlue,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Category",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Category",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Back Button - Modern Design
                OutlinedButton(
                    onClick = { navController.navigate("taskList") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.PrimaryBlue
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.5.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Back to Tasks",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Modal Bottom Sheet for Adding Category - Apple-style design
        if (isAddingCategory) {
            val bottomSheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = false
            )
            val hapticFeedback = LocalHapticFeedback.current

            LaunchedEffect(isAddingCategory) {
                if (isAddingCategory) {
                    bottomSheetState.show()
                }
            }

            ModalBottomSheet(
                onDismissRequest = {
                    isAddingCategory = false
                    newCategory = ""
                },
                sheetState = bottomSheetState,
                windowInsets = WindowInsets(0),
                dragHandle = {
                    // Custom drag handle - Apple style
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 36.dp, height = 4.dp),
                        shape = RoundedCornerShape(2.dp),
                        color = AppColors.TextSecondary.copy(alpha = 0.3f)
                    ) {}
                },
                containerColor = AppColors.CardBackground,
                contentColor = AppColors.TextPrimary,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                tonalElevation = 16.dp

            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    // Header Section - Apple style
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        // Cancel button (top-left)
                        TextButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                isAddingCategory = false
                                newCategory = ""
                            },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Text(
                                text = "Cancel",
                                color = AppColors.PrimaryBlue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Title (center)
                        Text(
                            text = "New Category",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // Add button (top-right)
                        // Add button (top-right)
                        TextButton(
                            onClick = {
                                if (newCategory.isNotBlank() && !categories.any { it.name.equals(newCategory, ignoreCase = true) }) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                                    val categoryToAdd = newCategory.trim()

                                    // Create category directly under root/categories
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val categoryRef = firebaseDb.getReference("categories").child(categoryToAdd)

                                            val categoryData = mapOf(
                                                "name" to categoryToAdd,
                                                "created" to System.currentTimeMillis(),
                                                "isCustom" to true,
                                                "taskCount" to 0
                                            )

                                            categoryRef.setValue(categoryData).await()
                                            Log.d("Firebase", "Created category: categories/$categoryToAdd")
                                        } catch (e: Exception) {
                                            Log.e("Firebase", "Failed to create category: ${e.message}")
                                        }
                                    }

                                    newCategory = ""
                                    isAddingCategory = false
                                    showSuccessAnimation = true

                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(800)
                                        showSuccessAnimation = false
                                    }
                                }
                            },
                            // ... rest of button code
                            // ... rest of button code
                            enabled = newCategory.isNotBlank() && !categories.any { it.name.equals(newCategory, ignoreCase = true) },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text(
                                text = "Add",
                                color = if (newCategory.isNotBlank() && !categories.any { it.name.equals(newCategory, ignoreCase = true) })
                                    AppColors.PrimaryBlue else AppColors.TextSecondary.copy(alpha = 0.5f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Category Icon Preview Section
                    val previewIcon = getCategoryIcon(newCategory.ifBlank { "New Category" })
                    val previewColor = getCategoryColor(newCategory.ifBlank { "New Category" })

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.LightBackground
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Category Preview",
                                fontSize = 14.sp,
                                color = AppColors.TextSecondary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Preview Icon
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                previewColor,
                                                previewColor.copy(alpha = 0.7f)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .animateContentSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = previewIcon,
                                    contentDescription = "Category Preview",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = newCategory.ifBlank { "Category Name" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Category Name Input Field - Modern Apple style
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Category Name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Label,
                                contentDescription = "Category name",
                                tint = AppColors.PrimaryBlue
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.PrimaryBlue,
                            focusedLabelColor = AppColors.PrimaryBlue,
                            unfocusedBorderColor = AppColors.TextSecondary.copy(alpha = 0.3f)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newCategory.isNotBlank() && !categories.any { it.name.equals(newCategory, ignoreCase = true) }) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                                    val categoryToAdd = newCategory.trim()

                                    // Create category directly under root/categories
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val categoryRef = firebaseDb.getReference("categories").child(categoryToAdd)

                                            val categoryData = mapOf(
                                                "name" to categoryToAdd,
                                                "created" to System.currentTimeMillis(),
                                                "isCustom" to true,
                                                "taskCount" to 0
                                            )

                                            categoryRef.setValue(categoryData).await()
                                            Log.d("Firebase", "Created category: categories/$categoryToAdd")
                                        } catch (e: Exception) {
                                            Log.e("Firebase", "Failed to create category: ${e.message}")
                                        }
                                    }

                                    newCategory = ""
                                    isAddingCategory = false
                                    showSuccessAnimation = true

                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(800)
                                        showSuccessAnimation = false
                                    }
                                }
                            }
                        )
                    )

                    // Category Suggestions - Horizontal scrolling cards
                    Text(
                        text = "Suggested Categories",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    val suggestions = listOf(
                        "Travel", "Food", "Fitness", "Study", "Finance",
                        "Entertainment", "Family", "Business", "Technology", "Hobby"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(suggestions) { suggestion ->
                            val suggestionIcon = getCategoryIcon(suggestion)
                            val suggestionColor = getCategoryColor(suggestion)

                            Card(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        newCategory = suggestion
                                    },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (newCategory == suggestion)
                                        suggestionColor else AppColors.TextSecondary.copy(alpha = 0.2f)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (newCategory == suggestion)
                                        suggestionColor.copy(alpha = 0.1f) else AppColors.CardBackground
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = suggestionIcon,
                                        contentDescription = suggestion,
                                        tint = suggestionColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = suggestion,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (newCategory == suggestion)
                                            suggestionColor else AppColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Tips Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.InfoBlue.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = AppColors.InfoBlue.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Tip",
                                tint = AppColors.InfoBlue,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Categories help you organize your tasks. The app will automatically choose an icon and color based on the category name.",
                                fontSize = 13.sp,
                                color = AppColors.TextPrimary.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Extra spacing at bottom for better UX
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Modern Success Animation Overlay
        AnimatedVisibility(
            visible = showSuccessAnimation,
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .blur(radius = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.SuccessGreen
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 12.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        AppColors.SuccessGreen,
                                        Color(0xFF0D9488) // Darker teal green for depth
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier
                                .size(60.dp)
                                .scale(
                                    animateFloatAsState(
                                        targetValue = if (showSuccessAnimation) 1f else 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        label = "checkScale"
                                    ).value
                                )
                        )
                    }
                }
            }
        }
    }
}