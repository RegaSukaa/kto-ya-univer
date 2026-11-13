package com.kstoya.univer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kstoya.univer.R
import com.kstoya.univer.ui.components.BigButton
import androidx.compose.ui.res.stringResource

@Composable
fun OnboardingScreen(
    onDone: (goLibrary: Boolean) -> Unit,
    onRecord: () -> Unit
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onDone(false) }) {
                Text(stringResource(R.string.onboarding_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(
                when (page) {
                    0 -> R.string.onboarding_1_title
                    1 -> R.string.onboarding_2_title
                    else -> R.string.onboarding_3_title
                }
            ),
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(
                when (page) {
                    0 -> R.string.onboarding_1_body
                    1 -> R.string.onboarding_2_body
                    else -> R.string.onboarding_3_body
                }
            ),
            fontSize = 17.sp,
            lineHeight = 25.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier.size(if (i == page) 22.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        if (page < 2) {
            BigButton(stringResource(R.string.onboarding_next), onClick = { page++ })
            Spacer(Modifier.height(8.dp))
            BigButton(stringResource(R.string.onboarding_play_placeholder), onClick = { onDone(false) }, primary = false)
        } else {
            BigButton(stringResource(R.string.onboarding_record), onClick = onRecord)
            Spacer(Modifier.height(8.dp))
            BigButton(stringResource(R.string.onboarding_add_chars), onClick = { onDone(true) }, primary = false)
        }
    }
}
