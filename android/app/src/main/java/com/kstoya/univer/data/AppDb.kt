package com.kstoya.univer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CharacterEntity::class, PlayerEntity::class, PresetEntity::class, SessionEntity::class, RoundEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun playerDao(): PlayerDao
    abstract fun presetDao(): PresetDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(ctx: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "ktya.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
