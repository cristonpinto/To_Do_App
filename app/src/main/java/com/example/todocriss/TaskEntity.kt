// File: F:\ToDocriss\app\src\main\java\com\example\todocriss\TaskEntity.kt
package com.example.todocriss

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "taskentity")
data class TaskEntity(
    @PrimaryKey val id: String,
    val text: String,
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM",
    val category: String = "Personal"
) {
    constructor() : this("", "", false, "MEDIUM", "Personal")
}