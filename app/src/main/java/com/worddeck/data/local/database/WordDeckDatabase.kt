package com.worddeck.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.worddeck.data.local.dao.DeckDao
import com.worddeck.data.local.dao.FlashcardDao
import com.worddeck.data.local.dao.ReviewEventDao
import com.worddeck.data.local.dao.ReviewStateDao
import com.worddeck.data.local.entity.DeckEntity
import com.worddeck.data.local.entity.FlashcardEntity
import com.worddeck.data.local.entity.ReviewEventEntity
import com.worddeck.data.local.entity.ReviewStateEntity

@Database(
    entities = [
        DeckEntity::class,
        FlashcardEntity::class,
        ReviewStateEntity::class,
        ReviewEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class WordDeckDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao

    abstract fun flashcardDao(): FlashcardDao

    abstract fun reviewStateDao(): ReviewStateDao

    abstract fun reviewEventDao(): ReviewEventDao
}
