package com.worddeck.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.worddeck.data.local.dao.DeckDao
import com.worddeck.data.local.entity.DeckEntity
import com.worddeck.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        DeckEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class WordDeckDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
}
