package ru.agent.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TestEntity)

    @Query("SELECT * FROM TestEntity")
    suspend fun getAll(): List<TestEntity>

    @Query("DELETE FROM TestEntity")
    suspend fun deleteAll()
}
