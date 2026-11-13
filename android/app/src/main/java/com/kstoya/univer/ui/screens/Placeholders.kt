package com.kstoya.univer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kstoya.univer.R
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.ui.components.BigButton
import com.kstoya.univer.ui.components.CharacterCard
import com.kstoya.univer.ui.components.toCardModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    characters: List<CharacterEntity>,
    onBack: () -> Unit,
    onRefresh: (((Int) -> Unit)) -> Unit
) {
    val ctx = LocalContext.current
    val assetFiles = remember { runCatching { ctx.assets.list("characters")?.toSet() ?: emptySet() }.getOrDefault(emptySet()) }
    val voiceFiles = remember {
        listOf("go", "guessed", "missed", "final").sumOf { slot ->
            runCatching {
                ctx.assets.list("voices/$slot")
                    ?.count { it.endsWith(".m4a", true) || it.endsWith(".wav", true) } ?: 0
            }.getOrDefault(0)
        }
    }
    val ownPhotos = characters.count { it.assetPhoto == null && it.photoPath != null }
    val stubs = characters.count { it.assetPhoto != null && !it.placeholder.not() && assetFiles.contains(it.assetPhoto?.substringAfterLast("/")) }
    val placeholders = characters.count { it.assetPhoto != null && (it.placeholder || (it.assetPhoto?.substringAfterLast("/")?.let { f -> assetFiles.contains(f) } == true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ph_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Text(
                    stringResource(R.string.ph_photos, ownPhotos, placeholders),
                    fontSize = 16.sp, fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.ph_voice_bank, voiceFiles),
                    fontSize = 16.sp, fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                BigButton(stringResource(R.string.ph_refresh), primary = false, onClick = { onRefresh { } })
                Spacer(Modifier.height(12.dp))
            }
            items(characters, key = { it.id }) { c ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(56.dp).aspectRatio(0.75f).clip(RoundedCornerShape(8.dp))) {
                        CharacterCard(card = c.toCardModel(), modifier = Modifier.fillMaxSize(), scale = 0.22f, photoFraction = 0.8f)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(c.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        val badge = when {
                            c.photoPath != null -> stringResource(R.string.ph_own)
                            c.assetPhoto == null -> stringResource(R.string.ph_missing)
                            c.placeholder -> stringResource(R.string.ph_stub)
                            else -> stringResource(R.string.ph_own)
                        }
                        val missing = c.assetPhoto != null && !assetFiles.contains(c.assetPhoto?.substringAfterLast("/"))
                        Text(
                            if (missing) stringResource(R.string.ph_missing) else badge,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.ph_howto_title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.ph_howto_1), fontSize = 13.sp)
                Text(stringResource(R.string.ph_howto_2), fontSize = 13.sp)
                Text(stringResource(R.string.ph_howto_3), fontSize = 13.sp)
                Text(stringResource(R.string.ph_howto_4), fontSize = 13.sp)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
