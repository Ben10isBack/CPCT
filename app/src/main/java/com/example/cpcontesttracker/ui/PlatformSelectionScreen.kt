package com.example.cpcontesttracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

val POPULAR_PLATFORMS = listOf(
    "codeforces.com",
    "leetcode.com",
    "codechef.com",
    "atcoder.jp",
    "geeksforgeeks.org",
    "hackerrank.com",
    "hackerearth.com"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformSelectionScreen(
    initialSelection: Set<String>,
    onSave: (Set<String>) -> Unit
) {
    var selected by remember { mutableStateOf(initialSelection) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Platforms") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Choose the platforms you want to track:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(POPULAR_PLATFORMS) { platform ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (selected.contains(platform)) {
                                    selected - platform
                                } else {
                                    selected + platform
                                }
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = selected.contains(platform),
                            onCheckedChange = null // Handled by Row click
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Clean up the name for display (e.g., "codeforces.com" -> "Codeforces")
                        val displayName = platform.substringBefore(".")
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                        Text(text = displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Button(
                onClick = { onSave(selected) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selected.isNotEmpty()
            ) {
                Text("Save & Continue")
            }
        }
    }
}
