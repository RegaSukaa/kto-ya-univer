package com.kstoya.univer.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kstoya.univer.AppViewModel
import com.kstoya.univer.R
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.media.PhotoImporter
import com.kstoya.univer.ui.components.BigButton
import com.kstoya.univer.ui.components.CardSurface
import com.kstoya.univer.ui.components.CharacterCard
import com.kstoya.univer.ui.components.ChipRow
import com.kstoya.univer.ui.components.SectionTitle
import com.kstoya.univer.ui.components.TagPill
import com.kstoya.univer.ui.components.toCardModel
import com.kstoya.univer.ui.theme.CardPresets
import com.kstoya.univer.util.Share
import com.kstoya.univer.util.initialsOf
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditScreen(
    characterId: String,
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit,
    vm: AppViewModel
) {
    val ctx = LocalContext.current
    val chars by vm.characters.collectAsState()
    val existing = chars.firstOrNull { it.id == characterId }
    val isNew = characterId.isBlank()
    val newId = remember { UUID.randomUUID().toString() }
    var draft by remember {
        mutableStateOf(
            if (isNew) CharacterEntity(
                id = newId, name = "", kind = "friend", bgPreset = 0, initials = "",
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), source = "local"
            ) else null
        )
    }
    LaunchedEffect(existing) { if (draft == null && existing != null) draft = existing }

    var photoMenu by remember { mutableStateOf(false) }
    var cropUri by remember { mutableStateOf<Uri?>(null) }
    var processing by remember { mutableStateOf(false) }
    var cutFailed by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { suggestions = vm.tagSuggestions() }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) cropUri = uri
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cropUri = cameraUri
    }

    val d = draft
    if (d == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val duplicateName = !isNew && chars.any { it.id != d.id && it.name.equals(d.name.trim(), true) } && d.name.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (isNew) R.string.edit_title_new else R.string.edit_title_edit),
                        fontWeight = FontWeight.SemiBold
                    )
                },
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
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.width(210.dp).aspectRatio(0.75f)) {
                CharacterCard(card = d.toCardModel(), modifier = Modifier.fillMaxSize(), scale = 0.55f)
                if (processing) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            BigButton(
                text = stringResource(if (d.photoPath == null && d.assetPhoto == null) R.string.edit_photo_add else R.string.edit_photo),
                onClick = { photoMenu = true },
                primary = false
            )
            if (cutFailed) Text(stringResource(R.string.cutout_failed), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = d.name,
                onValueChange = { v -> draft = d.copy(name = v.take(24)) },
                label = { Text(stringResource(R.string.edit_name)) },
                placeholder = { Text(stringResource(R.string.edit_name_hint)) },
                singleLine = true,
                supportingText = { Text("${d.name.length}/24") },
                modifier = Modifier.fillMaxWidth()
            )
            if (duplicateName) {
                Text(stringResource(R.string.edit_duplicate_name), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.edit_kind))
            Spacer(Modifier.height(8.dp))
            val kinds = listOf("friend", "teacher", "other")
            val kindLabels = listOf(
                stringResource(R.string.edit_kind_friend),
                stringResource(R.string.edit_kind_teacher),
                stringResource(R.string.edit_kind_other)
            )
            ChipRow(kindLabels, kinds.indexOf(d.kind).coerceAtLeast(0)) { i ->
                val k = kinds[i]
                val wasDefault = d.bgPreset == com.kstoya.univer.ui.theme.defaultPresetFor(d.kind)
                draft = d.copy(
                    kind = k,
                    bgPreset = if (wasDefault) com.kstoya.univer.ui.theme.defaultPresetFor(k) else d.bgPreset
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.edit_tags))
            Spacer(Modifier.height(8.dp))
            val tags = listOf(d.tag1, d.tag2, d.tag3)
            val hints = listOf(
                stringResource(R.string.edit_tag1_hint),
                stringResource(R.string.edit_tag2_hint),
                stringResource(R.string.edit_tag3_hint)
            )
            for (i in 0..2) {
                OutlinedTextField(
                    value = tags[i].orEmpty(),
                    onValueChange = { v ->
                        val t = v.take(40)
                        draft = when (i) {
                            0 -> d.copy(tag1 = t.ifBlank { null })
                            1 -> d.copy(tag2 = t.ifBlank { null })
                            else -> d.copy(tag3 = t.ifBlank { null })
                        }
                    },
                    placeholder = { Text(hints[i], fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            if (suggestions.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.take(2).forEach { sug ->
                        Box(Modifier.clickable {
                            draft = when {
                                d.tag1.isNullOrBlank() -> d.copy(tag1 = sug)
                                d.tag2.isNullOrBlank() -> d.copy(tag2 = sug)
                                else -> d.copy(tag3 = sug)
                            }
                        }) { TagPill(sug) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.edit_bg))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardPresets.forEachIndexed { i, p ->
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(p.brush())
                            .then(if (d.bgPreset == i) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                            .clickable { draft = d.copy(bgPreset = i) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.edit_consent), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(20.dp))
            BigButton(
                text = stringResource(R.string.edit_save),
                enabled = d.name.isNotBlank(),
                onClick = {
                    vm.saveCharacter(
                        d.copy(name = d.name.trim(), initials = d.initials.ifBlank { initialsOf(d.name) }, source = "local")
                    ) {
                        vm.playClick()
                        onBack()
                    }
                }
            )
            if (!isNew) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { deleteDialog = true }) {
                    Text(stringResource(R.string.edit_delete), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (photoMenu) {
        ModalBottomSheet(onDismissRequest = { photoMenu = false }) {
            Column(Modifier.padding(20.dp)) {
                BigButton(stringResource(R.string.edit_photo_gallery), onClick = {
                    photoMenu = false
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                })
                Spacer(Modifier.height(8.dp))
                BigButton(stringResource(R.string.edit_photo_camera), primary = false, onClick = {
                    photoMenu = false
                    val f = File(ctx.cacheDir, "shot_${System.currentTimeMillis()}.jpg")
                    val uri = runCatching { Share.uriFor(ctx, f) }.getOrNull()
                    if (uri != null) {
                        cameraUri = uri
                        takePicture.launch(uri)
                    }
                })
                Spacer(Modifier.height(8.dp))
                if (d.photoPath != null || d.assetPhoto != null) {
                    BigButton(stringResource(R.string.edit_photo_remove), primary = false, onClick = {
                        photoMenu = false
                        draft = d.copy(photoPath = null, assetPhoto = null, source = "local")
                    })
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    cropUri?.let { uri ->
        CropScreen(
            uri = uri,
            onCancel = { cropUri = null },
            onDone = { zoom, dx, dy, cutout ->
                cropUri = null
                processing = true
                cutFailed = false
                vm.processPhoto(uri, d.id, zoom, dx, dy, cutout, d.photoPath) { path ->
                    processing = false
                    if (path != null) draft = d.copy(photoPath = path, assetPhoto = null, source = "local")
                    else cutFailed = true
                }
            }
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text(stringResource(R.string.common_delete)) },
            text = { Text(stringResource(R.string.lib_delete_confirm, d.name)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteDialog = false
                    vm.deleteCharacter(d) { onOpenLibrary() }
                }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { message = null }) { Text(stringResource(R.string.common_ok)) } }
        )
    }
}

/** Полноэкранный кроп 3:4 с пинч-зумом и сдвигом. */
@Composable
fun CropScreen(
    uri: Uri,
    onCancel: () -> Unit,
    onDone: (zoom: Float, dx: Float, dy: Float, cutout: Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var dx by rememberSaveable { mutableFloatStateOf(0f) }
    var dy by rememberSaveable { mutableFloatStateOf(0f) }
    var cutout by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(uri) {
        val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            PhotoImporter.loadBitmap(ctx, uri, 1440)
        }
        bitmap = loaded?.asImageBitmap()
    }

    Box(Modifier.fillMaxSize().background(BrushVertical)) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.crop_title),
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                stringResource(R.string.crop_hint),
                fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Color.Black)
            ) {
                val density = LocalDensity.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }
                val base = if (wPx / hPx > 0.75f) hPx * 0.75f else wPx
                val baseH = base / 0.75f
                Box(
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                zoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                val maxShiftX = (wPx * (zoom - 1f)) / 2f + 1f
                                val maxShiftY = (hPx * (zoom - 1f)) / 2f + 1f
                                dx = (dx + pan.x / maxShiftX).coerceIn(-1f, 1f)
                                dy = (dy + pan.y / maxShiftY).coerceIn(-1f, 1f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    bitmap?.let { bmp ->
                        val shiftX = (wPx * (zoom - 1f)) / 2f
                        val shiftY = (hPx * (zoom - 1f)) / 2f
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().graphicsLayer(
                                scaleX = zoom, scaleY = zoom,
                                translationX = dx * shiftX, translationY = dy * shiftY
                            )
                        )
                    } ?: CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.edit_cutout), color = Color.White, fontSize = 16.sp)
                Switch(checked = cutout, onCheckedChange = { cutout = it })
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.common_cancel)) }
                Button(onClick = { onDone(zoom.coerceAtLeast(1f), dx, dy, cutout) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.crop_done))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private val BrushVertical = androidx.compose.ui.graphics.Brush.verticalGradient(
    listOf(Color(0xFF16161C), Color(0xFF0B0B0E))
)
