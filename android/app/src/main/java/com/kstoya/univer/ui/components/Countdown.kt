package com.kstoya.univer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun CountdownNumber(digit: Int, label: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = digit,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 1.4f)) togetherWith fadeOut()
            },
            label = "countdown"
        ) { d ->
            Text(
                text = if (d == 0) label else d.toString(),
                fontSize = if (d == 0) 64.sp else 200.sp,
                lineHeight = 210.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
