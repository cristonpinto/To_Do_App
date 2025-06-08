package com.example.todocriss

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT * FROM categoryentity")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categoryentity WHERE name = :name")
    suspend fun deleteCategory(name: String)
}