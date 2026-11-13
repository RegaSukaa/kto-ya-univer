package com.kstoya.univer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY createdAt ASC")
    fun allFlow(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters ORDER BY createdAt ASC")
    suspend fun all(): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun byId(id: String): CharacterEntity?

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(c: CharacterEntity)

    @Upsert
    suspend fun upsertAll(list: List<CharacterEntity>)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM characters")
    suspend fun deleteAll()
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY lastSeenAt DESC")
    suspend fun all(): List<PlayerEntity>

    @Upsert
    suspend fun upsertAll(list: List<PlayerEntity>)

    @Query("DELETE FROM players")
    suspend fun deleteAll()
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY createdAt DESC")
    suspend fun all(): List<PresetEntity>

    @Query("SELECT * FROM presets ORDER BY createdAt DESC LIMIT 1")
    suspend fun latest(): PresetEntity?

    @Upsert
    suspend fun upsert(p: PresetEntity)

    @Query("DELETE FROM presets")
    suspend fun deleteAll()
}

@Dao
interface SessionDao {
    @Upsert
    suspend fun upsertSession(s: SessionEntity)

    @Query("UPDATE sessions SET endedAt = :endedAt WHERE id = :id")
    suspend fun finishSession(id: String, endedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRound(r: RoundEntity): Long

    @Query("UPDATE rounds SET result = :result, markedAt = :at WHERE sessionId = :sessionId AND playerId = :playerId")
    suspend fun setResult(sessionId: String, playerId: String, result: String, at: Long)

    @Query("SELECT * FROM rounds WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun rounds(sessionId: String): List<RoundEntity>

    @Query("DELETE FROM rounds")
    suspend fun deleteAllRounds()

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()
}
