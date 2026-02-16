package ru.agent.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TestEntity(
    @PrimaryKey
    val id: String,
    val name: String
)
