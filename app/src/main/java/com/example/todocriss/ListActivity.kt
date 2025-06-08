package com.example.todocriss

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.todocriss.ui.theme.ToDoCrissTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.collectAsState
import com.google.firebase.FirebaseApp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import androidx.navigation.NavController // Added import
import kotlin.math.*



data class Task(
    val id: String = System.currentTimeMillis().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String = "Personal"
)

enum class TaskPriority(val color: Color, val label: String) {
    HIGH(Color(0xFFFF6B6B), "High"),
    MEDIUM(Color(0xFFFFD93D), "Medium"),
    LOW(Color(0xFF6BCF7F), "Low")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(navController: NavController, modifier: Modifier = Modifier) { // Added navController parameter
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val taskDao = db.taskDao()
    val tasks by taskDao.getAllTasks().collectAsState(initial = emptyList())
    var showSuccessMessage by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }
    // Initialize Firebase Database
    val firebaseDb = Firebase.database
    val tasksRef = firebaseDb.getReference("tasks")

    // Sync on startup
    LaunchedEffect(Unit) {
        try {
            val firebaseTasks = tasksRef.get().await().children.mapNotNull { it.getValue(TaskEntity::class.java) }
            val localTaskIds = tasks.map { it.id }
            firebaseTasks.forEach { firebaseTask ->
                if (!localTaskIds.contains(firebaseTask.id)) {
                    taskDao.insertTask(firebaseTask)
                    Log.d("FirebaseSync", "Inserted Firebase task: ${firebaseTask.id}")
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Startup sync failed: ${e.message}")
            e.printStackTrace()
        }
    }

    // Sync local changes
    LaunchedEffect(tasks) {
        tasks.forEach { task ->
            try {
                tasksRef.child(task.id).setValue(task).await()
                Log.d("FirebaseSync", "Synced task to Firebase: ${task.id}")
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Failed to sync task ${task.id}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newTask by remember { mutableStateOf("") }
    var newTaskPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var newTaskCategory by remember { mutableStateOf("Personal") }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var editedTask by remember { mutableStateOf("") }
    var editedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }

    val categories = remember { mutableStateListOf("All", "Personal", "Work", "Shopping", "Health") }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    var isContentVisible by remember { mutableStateOf(false) }
    var showWelcomeAnimation by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "backgroundOffset"
    )

    LaunchedEffect(Unit) {
        delay(500)
        showWelcomeAnimation = false
        delay(300)
        isContentVisible = true
    }

    val filteredTasks = remember(tasks, selectedCategory, searchQuery) {
        tasks.filter { task ->
            val matchesCategory = selectedCategory == "All" || task.category == selectedCategory
            val matchesSearch = searchQuery.isEmpty() || task.text.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }.map { Task(it.id, it.text, it.isCompleted, TaskPriority.valueOf(it.priority), it.category) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedBackground(backgroundOffset)
        AnimatedVisibility(
            visible = showWelcomeAnimation,
            exit = fadeOut(tween(800)) + scaleOut(tween(800))
        ) { WelcomeAnimation() }
        AnimatedVisibility(
            visible = isContentVisible,
            enter = fadeIn(tween(1000)) + slideInVertically(
                animationSpec = tween(1000, easing = FastOutSlowInEasing),
                initialOffsetY = { it }
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp)
            ) {
                GlassmorphismHeader(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedCategory = selectedCategory,
                    categories = categories,
                    onCategorySelect = { selectedCategory = it },
                    completedCount = tasks.count { it.isCompleted },
                    totalCount = tasks.size,
                    navController = navController // Added navController parameter
                )
                Spacer(modifier = Modifier.height(24.dp))
                TaskStatsRow(
                    highPriorityCount = tasks.count { it.priority == "HIGH" && !it.isCompleted },
                    completedCount = tasks.count { it.isCompleted },
                    totalTasksCount = tasks.size
                )
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (filteredTasks.isEmpty()) {
                        EmptyTasksPlaceholder()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredTasks) { task ->
                                EnhancedTaskItem(
                                    task = task,
                                    onTaskToggle = {
                                        val updatedTask = task.copy(isCompleted = !task.isCompleted)
                                        CoroutineScope(Dispatchers.IO).launch {
                                            taskDao.updateTask(
                                                TaskEntity(updatedTask.id, updatedTask.text, updatedTask.isCompleted, updatedTask.priority.name, updatedTask.category)
                                            )
                                            try {
                                                tasksRef.child(updatedTask.id).setValue(updatedTask).await()
                                                Log.d("FirebaseSync", "Toggled task synced: ${updatedTask.id}")
                                            } catch (e: Exception) {
                                                Log.e("FirebaseSync", "Failed to toggle task ${task.id}: ${e.message}")
                                                e.printStackTrace()
                                            }
                                        }
                                    },
                                    onTaskEdit = {
                                        taskToEdit = TaskEntity(task.id, task.text, task.isCompleted, task.priority.name, task.category)
                                        editedTask = task.text
                                        editedPriority = task.priority
                                    },
                                    onTaskDelete = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val taskEntity = TaskEntity(task.id, task.text, task.isCompleted, task.priority.name, task.category)
                                            taskDao.deleteTask(taskEntity)
                                            try {
                                                tasksRef.child(task.id).removeValue().await()
                                                Log.d("FirebaseSync", "Deleted task from Firebase: ${task.id}")
                                            } catch (e: Exception) {
                                                Log.e("FirebaseSync", "Failed to delete task ${task.id}: ${e.message}")
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally).size(60.dp),
                    containerColor = Color(0xFF667eea),
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (showAddDialog) {
            ModernAddTaskDialog(
                taskText = newTask,
                onTaskTextChange = { newTask = it },
                priority = newTaskPriority,
                onPriorityChange = { newTaskPriority = it },
                categories = categories.filter { it != "All" },
                selectedCategory = newTaskCategory,
                onCategoryChange = { newTaskCategory = it },
                onConfirm = { text, priority, category ->
                    if (text.isNotBlank()) {
                        val newTaskEntity = TaskEntity(
                            System.currentTimeMillis().toString(),
                            text,
                            false,
                            priority.name,
                            category
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                taskDao.insertTask(newTaskEntity)
                                tasksRef.child(newTaskEntity.id).setValue(newTaskEntity).await()
                                Log.d("FirebaseSync", "New task added to Firebase: ${newTaskEntity.id}")

                                // Show success feedback
                                CoroutineScope(Dispatchers.Main).launch {
                                    feedbackMessage = "Task added successfully! ✅"
                                    showSuccessMessage = true
                                    delay(3000) // Hide after 3 seconds
                                    showSuccessMessage = false
                                }
                            } catch (e: Exception) {
                                Log.e("FirebaseSync", "Failed to add task ${newTaskEntity.id}: ${e.message}")
                                e.printStackTrace()

                                // Show error feedback
                                CoroutineScope(Dispatchers.Main).launch {
                                    feedbackMessage = "Failed to add task ❌"
                                    showSuccessMessage = true
                                    delay(3000)
                                    showSuccessMessage = false
                                }
                            }
                        }
                        newTask = ""
                        newTaskPriority = TaskPriority.MEDIUM
                        newTaskCategory = "Personal"
                    }
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false; newTask = "" }
            )
            // Feedback Snackbar
            AnimatedVisibility(
                visible = showSuccessMessage,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Text(
                        text = feedbackMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (taskToEdit != null) {
            ModernEditTaskDialog(
                taskText = editedTask,
                onTaskTextChange = { editedTask = it },
                priority = editedPriority,
                onPriorityChange = { editedPriority = it },
                onConfirm = { text, priority ->
                    if (text.isNotBlank() && taskToEdit != null) {
                        val updatedTask = taskToEdit!!.copy(text = text, priority = priority.name)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                taskDao.updateTask(updatedTask)
                                tasksRef.child(updatedTask.id).setValue(updatedTask).await()
                                Log.d("FirebaseSync", "Edited task synced to Firebase: ${updatedTask.id}")
                            } catch (e: Exception) {
                                Log.e("FirebaseSync", "Failed to edit task ${updatedTask.id}: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    }
                    taskToEdit = null
                    editedTask = ""
                },
                onDismiss = { taskToEdit = null; editedTask = "" }
            )
        }
    }
}

@Composable
fun AnimatedBackground(offset: Float) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        // Gradient background
        val gradient = Brush.radialGradient(
            colors = listOf(
                Color(0xFF667eea).copy(alpha = 0.3f),
                Color(0xFF764ba2).copy(alpha = 0.2f),
                Color(0xFF1e3c72).copy(alpha = 0.1f)
            ),
            center = Offset(width * 0.3f, height * 0.2f),
            radius = width * 0.8f
        )

        drawRect(gradient)

        // Floating orbs
        for (i in 0..8) {
            val angle = offset + (i * PI / 4).toFloat()
            val x = width * 0.5f + cos(angle) * (width * 0.3f + i * 20)
            val y = height * 0.5f + sin(angle * 0.7f) * (height * 0.2f + i * 15)
            val radius = (30 + i * 8).toFloat()
            val alpha = 0.1f - (i * 0.01f)

            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun WelcomeAnimation() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "welcome")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 48.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Todo Criss",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Organize your life beautifully",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassmorphismHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    categories: List<String>,
    onCategorySelect: (String) -> Unit,
    completedCount: Int,
    totalCount: Int,
    navController: NavController
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0xFF667eea).copy(alpha = 0.25f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF667eea).copy(alpha = 0.3f),
                    Color(0xFF764ba2).copy(alpha = 0.2f)
                )
            )
        )
    ) {
        // Subtle gradient overlay for glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF667eea).copy(alpha = 0.05f),
                            Color(0xFF764ba2).copy(alpha = 0.03f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset.Infinite
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Hello! ☀️",
                            fontSize = 16.sp,
                            color = Color(0xFF667eea).copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Let's be productive",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3748),
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Enhanced Progress Circle
                    Box(
                        modifier = Modifier.size(70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

                        // Background circle
                        Canvas(modifier = Modifier.size(70.dp)) {
                            drawCircle(
                                color = Color(0xFFF7FAFC),
                                radius = size.minDimension / 2
                            )
                        }

                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(75.dp),
                            color = Color(0xFF667eea),
                            strokeWidth = 6.dp,
                            trackColor = Color(0xFFE2E8F0),
                            strokeCap = StrokeCap.Round
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$completedCount/$totalCount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D3748),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Enhanced Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            "Search your tasks...",
                            color = Color(0xFF718096),
                            fontSize = 16.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_search),
                            contentDescription = "Search",
                            tint = Color(0xFF667eea),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF667eea),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedTextColor = Color(0xFF2D3748),
                        unfocusedTextColor = Color(0xFF2D3748),
                        cursorColor = Color(0xFF667eea),
                        focusedContainerColor = Color.White.copy(alpha = 0.8f),
                        unfocusedContainerColor = Color(0xFFF7FAFC).copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Category Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4A5568)
                    )

                    // See All Button
                    TextButton(
                        onClick = { navController.navigate("categories") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "See All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF667eea)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ">",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF667eea)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Enhanced Category Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categories.forEach { category ->
                        EnhancedCategoryPill(
                            category = category,
                            isSelected = category == selectedCategory,
                            onClick = { onCategorySelect(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedCategoryPill(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF667eea) else Color.Transparent,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "categoryColor"
    )

    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF4A5568),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "textColor"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) Color.Transparent else Color(0xFFE2E8F0),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "borderColor"
    )

    val scale = remember { Animatable(1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.animateTo(
                targetValue = 0.95f,
                animationSpec = tween(100, easing = FastOutSlowInEasing)
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(100, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(20.dp))
            .background(animatedColor)
            .border(
                width = 1.5.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Add category icons for better UX
            val categoryIcon = when (category) {
                "All" -> "📋"
                "Personal" -> "👤"
                "Work" -> "💼"
                "Shopping" -> "🛒"
                "Health" -> "💊"
                else -> "📝"
            }

            Text(
                text = categoryIcon,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = category,
                color = animatedTextColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = 0.25.sp
            )
        }
    }
}

@Composable
fun TaskStatsRow(
    highPriorityCount: Int,
    completedCount: Int,
    totalTasksCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatsCard(
            title = "High Priority",
            value = highPriorityCount.toString(),
            icon = "🔥",
            color = Color(0xFFFF6B6B),
            modifier = Modifier.weight(1f)
        )

        StatsCard(
            title = "Completed",
            value = completedCount.toString(),
            icon = "✅",
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )

        StatsCard(
            title = "Total",
            value = totalTasksCount.toString(),
            icon = "📋",
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun EnhancedTaskItem(
    task: Task,
    onTaskToggle: () -> Unit,
    onTaskEdit: () -> Unit,
    onTaskDelete: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(task.isCompleted) {
        if (task.isCompleted) {
            scale.animateTo(
                targetValue = 0.98f,
                animationSpec = tween(
                    durationMillis = 200,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = task.priority.color.copy(alpha = 0.3f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                Color.White.copy(alpha = 0.7f) else Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(task.priority.color, CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onTaskToggle() }
            ) {
                if (task.isCompleted) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_edit),
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task Text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) Color.Gray else Color.Black,
                    textDecoration = if (task.isCompleted)
                        TextDecoration.LineThrough else TextDecoration.None
                )

                Text(
                    text = task.category,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Action Buttons
            Row {
                IconButton(onClick = onTaskEdit) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_edit),
                        contentDescription = "Edit",
                        tint = Color(0xFF667eea)
                    )
                }

                IconButton(onClick = onTaskDelete) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_delete),
                        contentDescription = "Delete",
                        tint = Color(0xFFFF6B6B)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTasksPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✨",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No tasks found",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add a new task or change your filters",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAddTaskDialog(
    taskText: String,
    onTaskTextChange: (String) -> Unit,
    priority: TaskPriority,
    onPriorityChange: (TaskPriority) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onConfirm: (String, TaskPriority, String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add New Task",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = taskText,
                    onValueChange = onTaskTextChange,
                    label = { Text("Task description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Priority Selection
                Text(
                    text = "Priority",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.values().forEach { taskPriority ->
                        val isSelected = priority == taskPriority
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) taskPriority.color.copy(alpha = 0.2f)
                                    else Color.LightGray.copy(alpha = 0.3f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) taskPriority.color else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onPriorityChange(taskPriority) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = taskPriority.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) taskPriority.color else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection
                Text(
                    text = "Category",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Replace the ExposedDropdownMenuBox section with this:
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = { }, // Keep empty for read-only
                        readOnly = true,
                        label = { Text("Select Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF667eea),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedLabelColor = Color(0xFF667eea),
                            unfocusedLabelColor = Color.Gray
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = category,
                                        color = Color.Black,
                                        fontSize = 16.sp
                                    )
                                },
                                onClick = {
                                    onCategoryChange(category)
                                    expanded = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Replace the button Row with this enhanced version:
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF667eea)
                        ),
                        border = BorderStroke(2.dp, Color(0xFF667eea))
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            // For Add Dialog: onConfirm(taskText, priority, selectedCategory)
                            // For Edit Dialog: onConfirm(taskText, priority)
                            onConfirm(taskText, priority, selectedCategory) // Use this line for Add Dialog
                            // onConfirm(taskText, priority) // Use this line for Edit Dialog instead
                        },
                        enabled = taskText.isNotBlank(), // Add validation
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF667eea),
                            disabledContainerColor = Color(0xFF667eea).copy(alpha = 0.5f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            text = "Add Task", // Change to "Save" for Edit Dialog
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernEditTaskDialog(
    taskText: String,
    onTaskTextChange: (String) -> Unit,
    priority: TaskPriority,
    onPriorityChange: (TaskPriority) -> Unit,
    onConfirm: (String, TaskPriority) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Edit Task",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = taskText,
                    onValueChange = onTaskTextChange,
                    label = { Text("Task description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF667eea),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedLabelColor = Color(0xFF667eea),
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color(0xFF667eea)
                    ),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    ),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Priority Selection
                Text(
                    text = "Priority",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.values().forEach { taskPriority ->
                        val isSelected = priority == taskPriority
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) taskPriority.color.copy(alpha = 0.2f)
                                    else Color.LightGray.copy(alpha = 0.3f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) taskPriority.color else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onPriorityChange(taskPriority) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = taskPriority.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) taskPriority.color else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Replace the button Row with this enhanced version:
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF667eea)
                        ),
                        border = BorderStroke(2.dp, Color(0xFF667eea))
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            // For Add Dialog: onConfirm(taskText, priority, selectedCategory)
                            // For Edit Dialog: onConfirm(taskText, priority)
                            onConfirm(taskText, priority) // Use this line for Add Dialog
                            // onConfirm(taskText, priority) // Use this line for Edit Dialog instead
                        },
                        enabled = taskText.isNotBlank(), // Add validation
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF667eea),
                            disabledContainerColor = Color(0xFF667eea).copy(alpha = 0.5f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            text = "Add Task", // Change to "Save" for Edit Dialog
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}