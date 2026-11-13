package com.kstoya.univer.ui.components

import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.data.assetPhotoPaths
import com.kstoya.univer.util.initialsOf
import java.io.File

data class CardModel(
    val id: String,
    val name: String,
    val tags: List<String>,
    val kind: String,
    val bgPreset: Int,
    val initials: String,
    val photoModel: Any?,
    val hasOwnPhoto: Boolean,
    /** путь внутри assets — такие картинки декодируем напрямую, без Coil */
    val assetPath: String? = null
)

fun CharacterEntity.toCardModel(photoIndex: Int = 0): CardModel {
    val local = photoPath?.let { p -> File(p).takeIf { it.exists() && it.length() > 0 } }
    val assets = assetPhotoPaths()
    val chosenAsset = if (assets.isEmpty()) null else assets[Math.floorMod(photoIndex, assets.size)]
    val asset = chosenAsset?.let { "file:///android_asset/$it" }
    return CardModel(
        id = id,
        name = name,
        tags = listOfNotNull(tag1, tag2, tag3).filter { it.isNotBlank() },
        kind = kind,
        bgPreset = bgPreset,
        initials = initials.ifBlank { initialsOf(name) },
        photoModel = local ?: asset,
        hasOwnPhoto = local != null,
        assetPath = if (local == null) chosenAsset else null
    )
}

fun fallbackCard(name: String, kind: String = "other"): CardModel =
    CardModel("x", name, emptyList(), kind, 0, initialsOf(name), null, false)
