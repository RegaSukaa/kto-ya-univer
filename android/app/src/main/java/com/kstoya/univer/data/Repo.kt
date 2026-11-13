package com.kstoya.univer.data

import android.content.Context
import android.net.Uri
import com.kstoya.univer.game.PlayerSlot
import com.kstoya.univer.game.RoundInfo
import com.kstoya.univer.util.initialsOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class SetCharacter(
    val id: String,
    val name: String,
    val tag1: String? = null,
    val tag2: String? = null,
    val tag3: String? = null,
    val kind: String = "other",
    val bgPreset: Int = 0,
    val initials: String = "",
    val photo: String? = null,
    val placeholder: Boolean = false
)

@Serializable
data class SetManifest(
    val format: String = "ktya/1",
    val exportedAt: Long = 0L,
    val title: String = "",
    val deckHash: String = "",
    val characters: List<SetCharacter> = emptyList()
)

data class ReadSet(val manifest: SetManifest, val photos: Map<String, ByteArray>)

data class ImportPreview(
    val title: String,
    val manifest: SetManifest,
    val photos: Map<String, ByteArray>,
    val conflicts: List<String>
)

enum class ImportPolicy { KEEP, REPLACE, DUPLICATE }

/** Экспорт/импорт набора .ktya: zip(manifest.json + photos/<id>.<ext>). */
class SetFile(private val ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    fun deckHash(chars: List<CharacterEntity>): String {
        val payload = chars.sortedBy { it.id }
            .joinToString("\n") { "${it.id}|${it.name}|${it.tag1.orEmpty()}|${it.tag2.orEmpty()}|${it.tag3.orEmpty()}|${it.kind}" }
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
        return "sha256:" + digest.joinToString("") { "%02x".format(it) }
    }

    fun writeSet(chars: List<CharacterEntity>, title: String, out: OutputStream): Int {
        val entities = chars.map { it.copy(photoPath = null) }
        val manifest = SetManifest(
            format = "ktya/1",
            exportedAt = System.currentTimeMillis(),
            title = title,
            deckHash = deckHash(entities),
            characters = entities.map {
                SetCharacter(
                    id = it.id, name = it.name, tag1 = it.tag1, tag2 = it.tag2, tag3 = it.tag3,
                    kind = it.kind, bgPreset = it.bgPreset, initials = it.initials,
                    photo = photoEntryName(it), placeholder = it.placeholder
                )
            }
        )
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(json.encodeToString(SetManifest.serializer(), manifest).toByteArray())
            zip.closeEntry()
            chars.forEach { c ->
                val bytes = photoBytes(c) ?: return@forEach
                zip.putNextEntry(ZipEntry(photoEntryName(c)))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return chars.size
    }

    fun readSet(input: InputStream): ReadSet {
        var manifest: SetManifest? = null
        val photos = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == "manifest.json") {
                    manifest = json.decodeFromString(SetManifest.serializer(), zip.readBytes().decodeToString())
                } else if (name.startsWith("photos/")) {
                    photos[name.removePrefix("photos/")] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val m = manifest ?: throw IllegalStateException("manifest.json missing")
        if (m.characters.isEmpty()) throw IllegalStateException("empty set")
        return ReadSet(m, photos)
    }

    private fun photoEntryName(c: CharacterEntity): String {
        val ext = when {
            c.photoPath?.endsWith(".png", true) == true -> "png"
            c.photoPath?.endsWith(".jpg", true) == true -> "jpg"
            c.photoPath?.endsWith(".jpeg", true) == true -> "jpg"
            c.assetPhoto?.endsWith(".png", true) == true -> "png"
            else -> "webp"
        }
        return "photos/${c.id}.$ext"
    }

    private fun photoBytes(c: CharacterEntity): ByteArray? {
        c.photoPath?.let { p ->
            val f = File(p)
            if (f.exists() && f.length() > 0) return runCatching { f.readBytes() }.getOrNull()
        }
        c.assetPhoto?.let { asset ->
            return runCatching { ctx.assets.open(asset).readBytes() }.getOrNull()
        }
        return null
    }
}

class Repo(
    private val ctx: Context,
    private val db: AppDb,
    private val prefs: Prefs
) {
    private val setFile = SetFile(ctx)
    val chars: Flow<List<CharacterEntity>> = db.characterDao().allFlow()

    suspend fun allCharacters(): List<CharacterEntity> = db.characterDao().all()
    suspend fun character(id: String): CharacterEntity? = db.characterDao().byId(id)
    suspend fun saveCharacter(c: CharacterEntity) = db.characterDao().upsert(c)
    suspend fun deleteCharacter(c: CharacterEntity) = withContext(Dispatchers.IO) {
        db.characterDao().delete(c.id)
        c.photoPath?.let { runCatching { File(it).delete() } }
        runCatching { File(File(ctx.filesDir, "cards"), "${c.id}_thumb.webp").delete() }
    }

    suspend fun tagSuggestions(): List<String> {
        val all = db.characterDao().all()
        return (all.mapNotNull { it.tag1 } + all.mapNotNull { it.tag2 } + all.mapNotNull { it.tag3 })
            .map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(12)
    }

    suspend fun players(): List<PlayerEntity> = db.playerDao().all()

    suspend fun savePlayers(names: List<String>) {
        if (names.isEmpty()) return
        val now = System.currentTimeMillis()
        db.playerDao().upsertAll(names.mapIndexed { i, n ->
            PlayerEntity(id = "p${i}_${n.hashCode()}", name = n.trim(), isHost = i == 0, lastSeenAt = now)
        })
    }

    suspend fun presets(): List<PresetEntity> = db.presetDao().all()
    suspend fun latestPreset(): PresetEntity? = db.presetDao().latest()

    suspend fun savePreset(title: String, names: List<String>) {
        val jsonNames = names.joinToString("\u0001")
        db.presetDao().upsert(PresetEntity(UUID.randomUUID().toString(), title, jsonNames, System.currentTimeMillis()))
    }

    fun presetNames(p: PresetEntity): List<String> = p.namesJson.split("\u0001").filter { it.isNotBlank() }

    suspend fun createSession(mode: String, playerCount: Int, showMs: Int, seed: Long, deckHash: String): String {
        val id = UUID.randomUUID().toString()
        db.sessionDao().upsertSession(
            SessionEntity(id, System.currentTimeMillis(), null, mode, playerCount, showMs, seed, deckHash)
        )
        return id
    }

    suspend fun finishSession(id: String) = db.sessionDao().finishSession(id, System.currentTimeMillis())

    suspend fun addRound(sessionId: String, order: Int, slot: PlayerSlot, characterId: String, characterName: String, showMs: Int) {
        db.sessionDao().addRound(
            RoundEntity(
                sessionId = sessionId, orderIndex = order, playerId = slot.id, playerName = slot.name,
                characterId = characterId, characterName = characterName, result = null, shownMs = showMs, markedAt = null
            )
        )
    }

    suspend fun setResult(sessionId: String, playerId: String, result: String) {
        db.sessionDao().setResult(sessionId, playerId, result, System.currentTimeMillis())
    }

    // ---------- банк из assets ----------

    @Serializable
    private data class AssetSounds(val win: String? = null, val skip: String? = null)

    @Serializable
    private data class AssetCharacter(
        val id: String, val name: String, val kind: String = "other", val bgPreset: Int = 0,
        val initials: String = "", val tags: List<String> = emptyList(), val photo: String? = null,
        val photos: List<String> = emptyList(),
        val sounds: AssetSounds? = null, val placeholder: Boolean = true
    )

    @Serializable
    private data class AssetManifest(val format: String = "", val version: Int = 1, val characters: List<AssetCharacter> = emptyList())

    private val bankJson = Json { ignoreUnknownKeys = true }

    private fun readManifest(): AssetManifest {
        val text = ctx.assets.open("characters.json").bufferedReader().use { it.readText() }
        return bankJson.decodeFromString(AssetManifest.serializer(), text)
    }

    /** Наполняет пустую базу банком. force=true — перечитывает assets, не трогая правки пользователя. */
    suspend fun seedFromAssets(force: Boolean = false): Int = withContext(Dispatchers.IO) {
        if (!force && db.characterDao().count() > 0) return@withContext db.characterDao().count()
        val manifest = runCatching { readManifest() }.getOrNull() ?: return@withContext 0
        val existing = db.characterDao().all().associateBy { it.id }
        val now = System.currentTimeMillis()
        val rows = manifest.characters.mapNotNull { c ->
            val old = existing[c.id]
            if (old != null && old.source == "local") return@mapNotNull null
            val tags = c.tags
            CharacterEntity(
                id = c.id,
                name = c.name,
                tag1 = tags.getOrNull(0),
                tag2 = tags.getOrNull(1),
                tag3 = tags.getOrNull(2),
                kind = c.kind,
                bgPreset = c.bgPreset,
                photoPath = old?.photoPath,
                assetPhoto = c.photos.firstOrNull() ?: c.photo,
                assetPhotos = c.photos.takeIf { it.isNotEmpty() }?.joinToString("\u0001"),
                initials = c.initials.ifBlank { initialsOf(c.name) },
                createdAt = old?.createdAt ?: now,
                updatedAt = now,
                source = "assets",
                placeholder = c.placeholder,
                assetSoundWin = c.sounds?.win,
                assetSoundSkip = c.sounds?.skip
            )
        }
        db.characterDao().upsertAll(rows)
        db.characterDao().count()
    }

    // ---------- импорт/экспорт ----------

    suspend fun exportTo(chars: List<CharacterEntity>, title: String, uri: Uri): Int = withContext(Dispatchers.IO) {
        ctx.contentResolver.openOutputStream(uri)?.use { out ->
            setFile.writeSet(chars, title, out)
        } ?: 0
    }

    fun exportToFile(chars: List<CharacterEntity>, title: String, file: File): Int {
        file.outputStream().use { setFile.writeSet(chars, title, it) }
        return chars.size
    }

    fun deckHash(chars: List<CharacterEntity>): String = setFile.deckHash(chars)

    suspend fun previewImport(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        val read = ctx.contentResolver.openInputStream(uri)?.use { setFile.readSet(it) }
            ?: throw IllegalStateException("cannot open")
        val localIds = db.characterDao().all().map { it.id }.toSet()
        ImportPreview(
            title = read.manifest.title,
            manifest = read.manifest,
            photos = read.photos,
            conflicts = read.manifest.characters.map { it.id }.filter { it in localIds }
        )
    }

    suspend fun applyImport(preview: ImportPreview, policy: ImportPolicy): Int = withContext(Dispatchers.IO) {
        val existing = db.characterDao().all().associateBy { it.id }
        val now = System.currentTimeMillis()
        val cardsDir = File(ctx.filesDir, "cards").apply { mkdirs() }
        var imported = 0
        preview.manifest.characters.forEach { sc ->
            val old = existing[sc.id]
            val newId = when {
                old == null -> sc.id
                policy == ImportPolicy.KEEP -> return@forEach
                policy == ImportPolicy.REPLACE -> sc.id
                else -> "${sc.id}_${UUID.randomUUID().toString().take(6)}"
            }
            val entry = sc.photo
            var savedPath: String? = null
            if (entry != null) {
                val fileName = entry.substringAfterLast("/")
                val bytes = preview.photos[fileName]
                if (bytes != null && bytes.isNotEmpty()) {
                    val ext = fileName.substringAfterLast('.', "webp")
                    val f = File(cardsDir, "${newId}_cut.$ext")
                    runCatching { f.writeBytes(bytes); savedPath = f.absolutePath }
                }
            }
            val entity = CharacterEntity(
                id = newId, name = sc.name, tag1 = sc.tag1, tag2 = sc.tag2, tag3 = sc.tag3,
                kind = sc.kind, bgPreset = sc.bgPreset, photoPath = savedPath,
                assetPhoto = null, initials = sc.initials.ifBlank { initialsOf(sc.name) },
                createdAt = old?.createdAt ?: now, updatedAt = now, source = "local",
                placeholder = false
            )
            db.characterDao().upsert(entity)
            imported++
        }
        imported
    }

    suspend fun importFrom(uri: Uri): ReadSet = withContext(Dispatchers.IO) {
        ctx.contentResolver.openInputStream(uri)?.use { setFile.readSet(it) } ?: throw IllegalStateException("cannot open")
    }

    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        db.characterDao().deleteAll()
        db.playerDao().deleteAll()
        db.presetDao().deleteAll()
        db.sessionDao().deleteAllRounds()
        db.sessionDao().deleteAllSessions()
        File(ctx.filesDir, "cards").deleteRecursively()
        File(ctx.filesDir, "voice").deleteRecursively()
        File(ctx.cacheDir, "cards").deleteRecursively()
        Unit
    }

    /** Размер filesDir — используется тестом «стереть данные». */
    fun filesDirSize(): Long = ctx.filesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun roundsOf(sessionId: String, chars: List<CharacterEntity>): List<RoundInfo> =
        emptyList()
}
