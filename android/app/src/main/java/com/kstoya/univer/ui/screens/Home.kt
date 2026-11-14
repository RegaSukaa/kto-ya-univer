package com.kstoya.univer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kstoya.univer.R
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.ui.components.BigButton

@Composable
fun HomeScreen(
    characters: List<CharacterEntity>,
    onLocal: () -> Unit,
    onOnline: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit,
    onRules: () -> Unit
) {
    val canPlay = characters.size >= 2
    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.logo_art),
            contentDescription = null,
            modifier = Modifier.size(104.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_hint),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))
        if (!canPlay) {
            Text(
                text = stringResource(R.string.home_need_chars),
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        BigButton(
            text = stringResource(R.string.home_local),
            onClick = { if (canPlay) onLocal() else onLibrary() },
            enabled = true,
            subtitle = if (canPlay) stringResource(R.string.home_hint) else stringResource(R.string.onboarding_2_button)
        )
        Spacer(Modifier.height(12.dp))
        BigButton(
            text = stringResource(R.string.rules_link),
            onClick = onRules,
            primary = false
        )
        Spacer(Modifier.height(12.dp))
        BigButton(
            text = stringResource(R.string.home_online),
            onClick = onOnline,
            primary = false
        )
        Spacer(Modifier.height(28.dp))
        BigButton(
            text = stringResource(R.string.home_library, characters.size),
            onClick = onLibrary,
            primary = false
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSettings) {
            Text(stringResource(R.string.home_settings), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.home_offline),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
