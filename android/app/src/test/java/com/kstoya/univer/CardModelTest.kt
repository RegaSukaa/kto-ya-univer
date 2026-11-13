package com.kstoya.univer

import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.data.assetPhotoPaths
import com.kstoya.univer.ui.components.toCardModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardModelTest {
    private fun person(photos: String?) = CharacterEntity(
        id = "friend_ivan", name = "Иван Петров", kind = "friend", initials = "ИП",
        assetPhoto = photos?.split('\u0001')?.firstOrNull(), assetPhotos = photos,
        createdAt = 1L, updatedAt = 1L
    )

    @Test
    fun `several photos are picked by index and wrap around`() {
        val c = person("characters/a/1.png\u0001characters/a/2.png\u0001characters/a/3.png")
        assertEquals(listOf("characters/a/1.png", "characters/a/2.png", "characters/a/3.png"), c.assetPhotoPaths())
        assertEquals("characters/a/1.png", c.toCardModel(0).assetPath)
        assertEquals("characters/a/2.png", c.toCardModel(1).assetPath)
        assertEquals("characters/a/3.png", c.toCardModel(2).assetPath)
        assertEquals("characters/a/1.png", c.toCardModel(3).assetPath)
        assertEquals("characters/a/3.png", c.toCardModel(-1).assetPath)
    }

    @Test
    fun `single legacy photo still works`() {
        val c = person(null).copy(assetPhoto = "characters/one.png")
        assertEquals(listOf("characters/one.png"), c.assetPhotoPaths())
        assertEquals("characters/one.png", c.toCardModel(5).assetPath)
    }

    @Test
    fun `no photo means initials`() {
        val c = person(null)
        assertNull(c.toCardModel().assetPath)
        assertNull(c.toCardModel().photoModel)
        assertEquals("ИП", c.toCardModel().initials)
    }
}
