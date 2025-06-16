package com.example.todocriss
import androidx.compose.foundation.BorderStroke



import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTasksScreen(navController: NavController, category: String) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val taskDao = db.taskDao()
    val tasks by taskDao.getAllTasks().collectAsState(initial = emptyList())
    val firebaseDb = Firebase.database
    val tasksRef = remember(category) { firebaseDb.getReference("tasks").child(category) }

    // State variables
    var showAddDialog by remember { mutableStateOf(false) }
    var newTaskText by remember { mutableStateOf("") }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var completedTasks by remember { mutableStateOf(setOf<String>()) }
    var selectedPriority by remember { mutableStateOf("MEDIUM") }

    // Get category icon and color
    fun getCategoryIcon(categoryName: String): ImageVector {
        return when (categoryName.lowercase()) {
            "personal" -> Icons.Default.Person
            "work" -> Icons.Default.Work
            "shopping" -> Icons.Default.ShoppingCart
            "health" -> Icons.Default.FavoriteBorder
            "travel", "traveling" -> Icons.Default.Flight
            "food" -> Icons.Default.Restaurant
            "fitness" -> Icons.Default.FitnessCenter
            "study", "education" -> Icons.Default.School
            "finance", "money" -> Icons.Default.AttachMoney
            "entertainment" -> Icons.Default.Movie
            "family" -> Icons.Default.People
            "business" -> Icons.Default.Business
            else -> Icons.Default.Category
        }
    }

    fun getCategoryColor(categoryName: String): Color {
        return when (categoryName.lowercase()) {
            "personal" -> AppColors.SuccessGreen
            "work" -> AppColors.AccentBlue
            "shopping" -> AppColors.WarningOrange
            "health" -> AppColors.ErrorRed
            "travel", "traveling" -> Color(0xFF3498DB)
            "food" -> Color(0xFFE67E22)
            "fitness" -> Color(0xFF2ECC71)
            "study", "education" -> Color(0xFF9B59B6)
            "finance", "money" -> Color(0xFFF1C40F)
            "entertainment" -> Color(0xFFE91E63)
            "family" -> Color(0xFF1ABC9C)
            "business" -> Color(0xFF34495E)
            else -> AppColors.PrimaryBlue
        }
    }

    val categoryIcon = getCategoryIcon(category)
    val categoryColor = getCategoryColor(category)

    // Sync tasks for the category
    LaunchedEffect(category) {
        try {
            val firebaseTasks = tasksRef.get().await().children.mapNotNull { snapshot ->
                snapshot.getValue(TaskEntity::class.java)?.copy(category = category)
            }
            val localTaskIds = tasks.map { it.id }
            firebaseTasks.forEach { task ->
                if (!localTaskIds.contains(task.id) && task.category == category) {
                    taskDao.insertTask(task)
                }
            }
            isLoading = false
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Task sync failed: ${e.message}")
            isLoading = false
        }
    }

    // Animation states
    val headerScale by animateFloatAsState(
        targetValue = if (showSuccessAnimation) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
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
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Beautiful Header with Category Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(headerScale)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(categoryColor, categoryColor.copy(alpha = 0.8f))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = category,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            val categoryTasks = tasks.filter { it.category == category }
                            val completedCount = categoryTasks.count { completedTasks.contains(it.id) }
                            Text(
                                text = "${categoryTasks.size} total • $completedCount completed",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            // Progress bar
                            if (categoryTasks.isNotEmpty()) {
                                val progress = completedCount.toFloat() / categoryTasks.size.toFloat()
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = category,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tasks List
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
                        CircularProgressIndicator(
                            color = categoryColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading tasks...",
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                val categoryTasks = tasks.filter { it.category == category }

                if (categoryTasks.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = "No tasks",
                                tint = AppColors.TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No tasks yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TextSecondary
                            )
                            Text(
                                text = "Tap the + button to add your first task",
                                fontSize = 14.sp,
                                color = AppColors.TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(categoryTasks) { index, task ->
                            val isCompleted = completedTasks.contains(task.id)
                            var isPressed by remember { mutableStateOf(false) }

                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "taskScale"
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
                                        .clickable {
                                            isPressed = true
                                            // Toggle completion
                                            completedTasks = if (isCompleted) {
                                                completedTasks - task.id
                                            } else {
                                                completedTasks + task.id
                                            }
                                        }
                                        .shadow(
                                            elevation = if (isCompleted) 2.dp else 4.dp,
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCompleted)
                                            AppColors.CardBackground.copy(alpha = 0.7f)
                                        else
                                            AppColors.CardBackground
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Checkbox
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    if (isCompleted) categoryColor else Color.Transparent,
                                                    CircleShape
                                                )
                                                .then(
                                                    if (!isCompleted) {
                                                        Modifier.background(
                                                            Color.Transparent,
                                                            CircleShape
                                                        )
                                                    } else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCompleted) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Completed",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .background(
                                                            Color.Transparent,
                                                            CircleShape
                                                        )
                                                        .clip(CircleShape)
                                                        .background(
                                                            categoryColor.copy(alpha = 0.2f),
                                                            CircleShape
                                                        )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        // Task text
                                        Text(
                                            text = task.text,
                                            fontSize = 16.sp,
                                            fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Medium,
                                            color = if (isCompleted)
                                                AppColors.TextSecondary.copy(alpha = 0.6f)
                                            else
                                                AppColors.TextPrimary,
                                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Delete button
                                        IconButton(
                                            onClick = {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    try {
                                                        taskDao.deleteTask(task)
                                                        tasksRef.child(task.id).removeValue().await()
                                                        Log.d("FirebaseSync", "Deleted task: ${task.id}")
                                                    } catch (e: Exception) {
                                                        Log.e("FirebaseSync", "Failed to delete task: ${e.message}")
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete task",
                                                tint = AppColors.ErrorRed.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Reset pressed state
                            LaunchedEffect(isPressed) {
                                if (isPressed) {
                                    delay(100)
                                    isPressed = false
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Back button
                OutlinedButton(
                    onClick = { navController.navigate("categories") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = categoryColor
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Back",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Add task button
                Button(
                    onClick = {
                        showAddDialog = true

                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = categoryColor
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Task",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Success Animation Overlay
        AnimatedVisibility(
            visible = showSuccessAnimation,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = AppColors.SuccessGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(20.dp)
                    )
                }
            }
        }
    }

    // Apple-style Modal Bottom Sheet for Adding Task
    if (showAddDialog) {
        val bottomSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false
        )

        LaunchedEffect(showAddDialog) {
            if (showAddDialog) {
                bottomSheetState.show()
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                showAddDialog = false
                newTaskText = ""
            },
            sheetState = bottomSheetState,
            windowInsets = WindowInsets(0),
            dragHandle = {
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    TextButton(
                        onClick = {
                            showAddDialog = false
                            newTaskText = ""
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = "Category",
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Task for $category",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                    }

                    TextButton(
                        onClick = {
                            if (newTaskText.isNotBlank()) {
                                val newTask = TaskEntity(
                                    id = System.currentTimeMillis().toString(),
                                    text = newTaskText,
                                    category = category,
                                    priority = selectedPriority // Use selected priority
                                )
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        taskDao.insertTask(newTask)
                                        tasksRef.child(newTask.id).setValue(newTask).await()
                                        Log.d("FirebaseSync", "Added task to $category: ${newTask.id}")
                                    } catch (e: Exception) {
                                        Log.e("FirebaseSync", "Failed to add task to $category: ${e.message}")
                                    }
                                }
                                newTaskText = ""
                                showAddDialog = false
                                showSuccessAnimation = true

                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(500)
                                    showSuccessAnimation = false
                                }
                            }
                        },
                        enabled = newTaskText.isNotBlank(),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(
                            text = "Add",
                            color = if (newTaskText.isNotBlank())
                                categoryColor else AppColors.TextSecondary.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Task Preview Card
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
                            .padding(20.dp)
                    ) {
                        // Task Preview Header
                        Text(
                            text = "Task Preview",
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Task Preview Content
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            // Checkbox preview
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        Color.Transparent,
                                        CircleShape
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        categoryColor.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Task text preview
                            Text(
                                text = if (newTaskText.isBlank()) "Your task will appear here" else newTaskText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (newTaskText.isBlank())
                                    AppColors.TextSecondary.copy(alpha = 0.6f)
                                else
                                    AppColors.TextPrimary
                            )
                        }
                    }
                }

                // Priority Selection
                var selectedPriority by remember { mutableStateOf("Medium") } // State for priority
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Low", "Medium", "High").forEach { priority ->
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    selectedPriority = priority
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedPriority == priority)
                                categoryColor.copy(alpha = 0.2f)
                            else
                                AppColors.CardBackground,
                            border = BorderStroke(
                                1.dp,
                                if (selectedPriority == priority) categoryColor else AppColors.TextSecondary.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = priority,
                                fontSize = 14.sp,
                                fontWeight = if (selectedPriority == priority) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selectedPriority == priority) categoryColor else AppColors.TextPrimary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                // Task Input Field - Modern Apple style
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    label = { Text("Task Description") },
                    placeholder = { Text("What needs to be done?") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "Task",
                            tint = categoryColor
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = categoryColor,
                        focusedLabelColor = categoryColor,
                        unfocusedBorderColor = AppColors.TextSecondary.copy(alpha = 0.3f),
                        cursorColor = categoryColor
                    )
                )

                // Add some spacing at the bottom
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}