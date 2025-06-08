package com.example.todocriss

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categoryentity")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val iconName: String, // Store icon name as a string (e.g., "Work", "Person")
    val colorHex: String, // Store color as hex (e.g., "#4A90E2")
    val taskCount: Int = 0
)