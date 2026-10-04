package com.example.notes.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, dueDate ASC, id DESC")
    fun getTodosFlow(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Long): TodoEntity?

    @Query("SELECT * FROM todos WHERE isCompleted = 0 AND latitude IS NOT NULL AND longitude IS NOT NULL")
    suspend fun getActiveTodosWithLocation(): List<TodoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity): Int

    @Delete
    suspend fun deleteTodo(todo: TodoEntity): Int

    @Query("UPDATE todos SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTodoCompletion(id: Long, isCompleted: Boolean): Int
}
