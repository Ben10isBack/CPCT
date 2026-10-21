package com.example.cpcontesttracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cpcontesttracker.R

/**
 * Lets the user pick platforms. Shown once on first launch (no [onBack]) and later from the
 * main menu, where [onBack] adds a back arrow and handles the system back gesture.
 */
@Composable
fun PlatformSelectionScreen(
    initialSelection: Set<String>,
    onSave: (Set<String>) -> Unit,
    onBack: (() -> Unit)? = null
) {
    var selected by remember { mutableStateOf(initialSelection) }
    if (onBack != null) BackHandler(onBack = onBack)

    Surface(color = DarkBackground, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp).offset(x = (-12).dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            } else {
                Spacer(Modifier.height(24.dp))
            }
            Image(
                painter = painterResource(R.drawable.ic_cpct_logo),
                contentDescription = "CPCT",
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Choose platforms",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "CPCT will track upcoming contests from these sites.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(Platform.selectable, key = { it.host }) { platform ->
                    val isChecked = platform.host in selected
                    Surface(
                        onClick = {
                            selected = if (isChecked) selected - platform.host else selected + platform.host
                        },
                        color = CardBackgroundDark,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isChecked) Accent else CardBackgroundRaised),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            PlatformLogo(platform, size = 40)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(platform.displayName, color = TextPrimary)
                                Text(platform.host, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null, // Handled by the row click
                                colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = DarkBackground)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onSave(selected) },
                enabled = selected.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(52.dp)
            ) {
                Text(if (onBack != null) "Save" else "Save & Continue")
            }
        }
    }
}
