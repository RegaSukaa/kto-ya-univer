package com.kstoya.univer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.kstoya.univer.data.ImportPolicy
import com.kstoya.univer.data.ImportPreview
import com.kstoya.univer.ui.components.BigButton
import com.kstoya.univer.ui.components.CharacterCard
import com.kstoya.univer.ui.components.ChipRow
import com.kstoya.univer.ui.components.toCardModel
import com.kstoya.univer.util.Share
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    characters: List<CharacterEntity>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (CharacterEntity) -> Unit,
    onDeleted: (CharacterEntity) -> Unit,
    onDuplicate: (CharacterEntity) -> Unit,
    onExport: (List<CharacterEntity>, String) -> File?,
    onImport: (android.net.Uri, (ImportPreview?) -> Unit) -> Unit,
    onApplyImport: (ImportPreview, ImportPolicy, (Int) -> Unit) -> Unit,
    onPlay: () -> Unit
) {
    val ctx = LocalContext.current
    val exportTitle = stringResource(R.string.share_subject)
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var sort by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<CharacterEntity?>(null) }
    var deleteFor by remember { mutableStateOf<CharacterEntity?>(null) }
    var preview by remember { mutableStateOf<ImportPreview?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri) { p ->
            if (p == null) message = ctx.getString(R.string.import_failed)
            else if (p.conflicts.isEmpty()) {
                onApplyImport(p, ImportPolicy.REPLACE) { n -> message = ctx.getString(R.string.import_done, n) }
            } else preview = p
        }
    }

    val filtered = characters
        .filter {
            when (filter) {
                1 -> it.kind == "friend"
                2 -> it.kind == "teacher"
                else -> true
            }
        }
        .filter {
            query.isBlank() || it.name.contains(query, true) ||
                listOfNotNull(it.tag1, it.tag2, it.tag3).any { t -> t.contains(query, true) }
        }
        .let { list ->
            when (sort) {
                0 -> list.sortedBy { it.name.lowercase() }
                1 -> list.sortedByDescending { it.createdAt }
                else -> list.sortedBy { it.kind }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.lib_title), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(
                                R.string.lib_count,
                                characters.size,
                                characters.count { it.kind == "friend" },
                                characters.count { it.kind == "teacher" }
                            ),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lib_import)) },
                            onClick = { menuOpen = false; importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lib_export)) },
                            onClick = {
                                menuOpen = false
                                val f = onExport(characters, exportTitle)
                                if (f == null) message = ctx.getString(R.string.export_empty)
                                else Share.file(ctx, f, "application/zip", ctx.getString(R.string.export_title))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lib_sort_name)) },
                            onClick = { sort = 0; menuOpen = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lib_sort_date)) },
                            onClick = { sort = 1; menuOpen = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lib_sort_kind)) },
                            onClick = { sort = 2; menuOpen = false }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.lib_add)) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.lib_search_hint), fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            ChipRow(
                items = listOf(
                    stringResource(R.string.lib_filter_all),
                    stringResource(R.string.lib_filter_friends),
                    stringResource(R.string.lib_filter_teachers)
                ),
                selected = filter,
                onSelect = { filter = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (characters.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.lib_empty), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    BigButton(stringResource(R.string.lib_add), onClick = onAdd)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { c ->
                        Column {
                            CharacterCard(
                                card = c.toCardModel(),
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = { onEdit(c) },
                                        onLongClick = { selected = c }
                                    ),
                                scale = 0.55f
                            )
                            if (c.assetPhoto == null && c.photoPath == null) {
                                Text(
                                    stringResource(R.string.lib_no_photo),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { c ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(c.name) },
            text = {
                Column {
                    TextButton(onClick = { selected = null; onEdit(c) }) { Text(stringResource(R.string.lib_edit)) }
                    TextButton(onClick = { selected = null; onDuplicate(c) }) { Text(stringResource(R.string.lib_duplicate)) }
                    TextButton(onClick = { selected = null; deleteFor = c }) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    deleteFor?.let { c ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text(stringResource(R.string.common_delete)) },
            text = { Text(stringResource(R.string.lib_delete_confirm, c.name)) },
            confirmButton = {
                TextButton(onClick = { onDeleted(c); deleteFor = null }) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    preview?.let { p ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(stringResource(R.string.import_title)) },
            text = { Text(stringResource(R.string.import_conflict) + " (" + p.conflicts.size + ")") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        onApplyImport(p, ImportPolicy.REPLACE) { n -> message = ctx.getString(R.string.import_done, n) }
                        preview = null
                    }) { Text(stringResource(R.string.import_replace)) }
                    TextButton(onClick = {
                        onApplyImport(p, ImportPolicy.DUPLICATE) { n -> message = ctx.getString(R.string.import_done, n) }
                        preview = null
                    }) { Text(stringResource(R.string.import_duplicate)) }
                    TextButton(onClick = {
                        onApplyImport(p, ImportPolicy.KEEP) { n -> message = ctx.getString(R.string.import_done, n) }
                        preview = null
                    }) { Text(stringResource(R.string.import_keep)) }
                }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    message?.let { m ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text(stringResource(R.string.lib_title)) },
            text = { Text(m) },
            confirmButton = { TextButton(onClick = { message = null }) { Text(stringResource(R.string.common_ok)) } }
        )
    }
}
