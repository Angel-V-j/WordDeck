package com.worddeck.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.worddeck.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WordDeckDatabase : RoomDatabase()
