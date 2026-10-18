package com.example.cpcontesttracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cpcontesttracker.R
import kotlinx.coroutines.delay

/** Short branded startup screen with a quick sync bar. Contests load in the background meanwhile. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing))
        delay(150)
        onFinished()
    }

    Surface(color = DarkBackground, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_cpct_logo),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(24.dp))
            Image(
                painter = painterResource(R.drawable.ic_cpct_wordmark),
                contentDescription = "CPCT",
                modifier = Modifier.height(32.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Competitive Programming\nContest Tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            LinearProgressIndicator(
                progress = { progress.value },
                color = Accent,
                trackColor = CardBackgroundRaised,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (progress.value < 1f) "Syncing contests…" else "Ready",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
