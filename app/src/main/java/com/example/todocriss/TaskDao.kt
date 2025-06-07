// File: F:\ToDocriss\app\src\main\java\com\example\todocriss\TaskDao.kt
package com.example.todocriss

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM taskentity")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM taskentity WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?
}