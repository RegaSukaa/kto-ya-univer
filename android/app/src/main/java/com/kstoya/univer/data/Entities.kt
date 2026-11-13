package com.kstoya.univer.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "characters", indices = [Index("kind"), Index("name")])
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tag1: String? = null,
    val tag2: String? = null,
    val tag3: String? = null,
    val kind: String = "other",
    val bgPreset: Int = 0,
    val photoPath: String? = null,
    val assetPhoto: String? = null,
    val cropJson: String? = null,
    val initials: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    /** "assets" — пришло из банка, "local" — добавлено/изменено пользователем */
    val source: String = "local",
    val placeholder: Boolean = false,
    val assetSoundWin: String? = null,
    val assetSoundSkip: String? = null
)

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isHost: Boolean = false,
    val lastSeenAt: Long = 0L
)

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val title: String,
    val namesJson: String,
    val createdAt: Long
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val mode: String,
    val playerCount: Int,
    val showMs: Int,
    val seed: Long,
    val deckHash: String
)

@Entity(tableName = "rounds", indices = [Index("sessionId")])
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val orderIndex: Int,
    val playerId: String,
    val playerName: String,
    val characterId: String,
    val characterName: String,
    val result: String?,
    val shownMs: Int,
    val markedAt: Long? = null
)
