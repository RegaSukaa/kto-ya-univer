package com.kstoya.univer.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.kstoya.univer.R
import com.kstoya.univer.ui.theme.presetFor

@Composable
fun kindLabel(kind: String): String = when (kind) {
    "friend" -> stringResource(R.string.edit_kind_friend)
    "teacher" -> stringResource(R.string.edit_kind_teacher)
    else -> stringResource(R.string.edit_kind_other)
}

/**
 * Главный элемент игры. Один компонент на все экраны: в игре scale=1,
 * в разборе 0.5, в превью редактора 0.55.
 */
@Composable
fun CharacterCard(
    card: CardModel,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    roundLabel: String? = null,
    typeLabel: String? = null,
    photoFraction: Float = 0.60f,
    showType: Boolean = true
) {
    val corner = (28 * scale).dp
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier.clip(shape).background(presetFor(card.kind, card.bgPreset).brush())
    ) {
        val nameSize = when {
            card.name.length <= 10 -> 64f
            card.name.length <= 16 -> 54f
            card.name.length <= 22 -> 44f
            else -> 38f
        } * scale
        Column(
            modifier = Modifier.fillMaxSize().padding((18 * scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showType || roundLabel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (showType) {
                        Text(
                            text = (typeLabel ?: kindLabel(card.kind)).uppercase(),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = (12 * scale).sp,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    if (roundLabel != null) {
                        Text(
                            text = roundLabel,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = (12 * scale).sp
                        )
                    }
                }
            }
            Spacer(Modifier.height((8 * scale).dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .fillMaxHeight(photoFraction)
                    .shadow((12 * scale).dp, RoundedCornerShape((24 * scale).dp), clip = false)
                    .border((4 * scale).dp, Color.White, RoundedCornerShape((24 * scale).dp))
                    .clip(RoundedCornerShape((24 * scale).dp)),
                contentAlignment = Alignment.Center
            ) {
                PhotoOrInitials(card, scale)
            }
            Spacer(Modifier.height((10 * scale).dp))
            Text(
                text = card.name,
                color = Color.White,
                fontSize = nameSize.sp,
                lineHeight = (nameSize * 1.05f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5 * scale).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (card.tags.isNotEmpty()) {
                Spacer(Modifier.height((6 * scale).dp))
                Text(
                    text = card.tags.joinToString(" · "),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = (15 * scale).sp,
                    lineHeight = (19 * scale).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PhotoOrInitials(card: CardModel, scale: Float) {
    val ctx = LocalContext.current
    val assetBitmap = remember(card.assetPath) {
        card.assetPath?.let { path ->
            runCatching { ctx.assets.open(path).use { BitmapFactory.decodeStream(it) } }.getOrNull()
        }
    }
    val initialsBlock: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = card.initials.ifBlank { "?" },
                color = Color.White,
                fontSize = (120 * scale).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    if (assetBitmap != null) {
        Image(
            bitmap = assetBitmap.asImageBitmap(),
            contentDescription = card.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    } else if (card.photoModel == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            initialsBlock()
            Text(
                text = stringResource(R.string.lib_no_photo),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = (13 * scale).sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = (8 * scale).dp)
            )
        }
    } else {
        SubcomposeAsyncImage(
            model = card.photoModel,
            contentDescription = card.name,
            contentScale = ContentScale.Fit,
            loading = { initialsBlock() },
            error = { initialsBlock() },
            success = { SubcomposeAsyncImageContent() },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Пунктирная миниатюра без фото для пустых состояний. */
@Composable
fun EmptyPhotoTile(initials: String, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF26262F)),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = Color.White.copy(alpha = 0.7f), fontSize = (size.value / 3).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TagPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun VerticalSpacer(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun HorizontalSpacer(w: Dp) = Spacer(Modifier.width(w))
