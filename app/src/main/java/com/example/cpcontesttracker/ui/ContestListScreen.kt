package com.example.cpcontesttracker.ui

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cpcontesttracker.data.model.Contest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestListScreen(
    viewModel: ContestViewModel,
    onFilterClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CP Contests") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(onClick = onFilterClick) {
                        Text("Platforms", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is ContestUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ContestUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                is ContestUiState.Success -> {
                    if (state.contests.isEmpty()) {
                        Text(
                            text = "No upcoming contests found.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.contests) { contest ->
                                ContestCard(contest)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContestCard(contest: Contest) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = contest.event,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Platform: ${contest.resource}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Starts: ${formatDate(contest.start)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Ends: ${formatDate(contest.end)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Button(onClick = { addContestToCalendar(context, contest) }) {
                    Text("Add to Calendar")
                }
            }
        }
    }
}

// Helper function to launch the Calendar Intent
private fun addContestToCalendar(context: Context, contest: Contest) {
    try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        val startDate = inputFormat.parse(contest.start)
        val endDate = inputFormat.parse(contest.end)

        if (startDate != null && endDate != null) {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDate.time)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDate.time)
                putExtra(CalendarContract.Events.TITLE, contest.event)
                putExtra(CalendarContract.Events.DESCRIPTION, "Contest URL: ${contest.href}")
                putExtra(CalendarContract.Events.EVENT_LOCATION, contest.resource)
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Helper function to format UTC time from Clist to local time
fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString)

        val outputFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault() // Convert to user's local timezone
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
