package com.example.cpcontesttracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cpcontesttracker.R
import com.example.cpcontesttracker.utils.CalendarInfo
import kotlinx.coroutines.delay
import java.time.LocalDate

private val CALENDAR_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CALENDAR,
    Manifest.permission.WRITE_CALENDAR
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestListScreen(
    viewModel: ContestViewModel,
    onFilterClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val calendarState by viewModel.calendarState.collectAsStateWithLifecycle()
    val isAutoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var dialogContest by remember { mutableStateOf<ContestItem?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        viewModel.onCalendarPermissionResult(granted)
        if (!granted) {
            Toast.makeText(context, "Calendar permission denied. You can enable it in Settings.", Toast.LENGTH_SHORT).show()
        }
    }
    val requestCalendarPermission = { permissionLauncher.launch(CALENDAR_PERMISSIONS) }

    // Ask for calendar access once (not again on every rotation)
    var askedForPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val granted = CALENDAR_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            viewModel.onCalendarPermissionResult(true)
        } else if (!askedForPermission) {
            askedForPermission = true
            requestCalendarPermission()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // One clock for the whole list, ticking once a minute for the countdowns
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(60_000)
            value = System.currentTimeMillis()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_cpct_logo),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Image(
                            painter = painterResource(R.drawable.ic_cpct_wordmark),
                            contentDescription = "CPCT",
                            modifier = Modifier.height(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
                actions = {
                    CalendarStatusButton(
                        calendarState = calendarState,
                        isAutoSyncEnabled = isAutoSyncEnabled,
                        onRequestPermission = requestCalendarPermission,
                        onAutoSyncChange = viewModel::setAutoSync,
                        onSyncNow = viewModel::syncNow
                    )
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                    }
                    IconButton(onClick = onFilterClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Choose platforms", tint = TextSecondary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is ContestUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextSecondary)
                }
                is ContestUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Couldn't load contests", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(state.message, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = viewModel::refresh) { Text("Try again") }
                    }
                }
                is ContestUiState.Success -> {
                    val today = LocalDate.now().toEpochDay()
                    // Group by day; recomputed at most once a minute when the clock ticks
                    val groups = remember(state.contests, now) {
                        state.contests.groupBy { groupLabel(it, now, today) }
                    }

                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (state.isOffline) {
                                item(key = "offline", contentType = "note") {
                                    Text(
                                        "Offline. Showing saved contests.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (state.contests.isEmpty()) {
                                item(key = "empty", contentType = "note") {
                                    Text(
                                        text = "No upcoming contests for your platforms.",
                                        color = TextSecondary,
                                        modifier = Modifier.padding(vertical = 32.dp)
                                    )
                                }
                            }

                            groups.forEach { (label, contests) ->
                                item(key = "header_$label", contentType = "header") {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                                    )
                                }
                                items(
                                    items = contests,
                                    key = { it.contest.id }, // Stable keys keep scrolling smooth
                                    contentType = { "contest" }
                                ) { item ->
                                    ContestCard(
                                        item = item,
                                        now = now,
                                        isSynced = item.contest.id in calendarState.synced,
                                        onCalendarClick = { dialogContest = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    dialogContest?.let { item ->
        CalendarInfoDialog(
            item = item,
            hasPermission = calendarState.hasPermission,
            syncedIn = calendarState.synced[item.contest.id],
            target = calendarState.target,
            onGrant = {
                dialogContest = null
                requestCalendarPermission()
            },
            onAdd = { viewModel.addToCalendar(item) },
            onDismiss = { dialogContest = null }
        )
    }
}

private fun groupLabel(item: ContestItem, now: Long, today: Long): String = when {
    item.startMs <= now -> "Live now"
    item.startEpochDay == today -> "Today"
    item.startEpochDay == today + 1 -> "Tomorrow"
    else -> item.dayText
}

/** Top-bar chip showing the sync state and account; tapping it opens the sync options. */
@Composable
private fun CalendarStatusButton(
    calendarState: CalendarState,
    isAutoSyncEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onSyncNow: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val target = calendarState.target

    val title: String
    val subtitle: String?
    when {
        !calendarState.hasPermission -> { title = "Calendar off"; subtitle = null }
        target == null -> { title = "No calendar"; subtitle = null }
        else -> { title = if (isAutoSyncEnabled) "Auto-sync on" else "Auto-sync off"; subtitle = target.provider }
    }

    Box {
        Surface(
            onClick = { if (calendarState.hasPermission) expanded = true else onRequestPermission() },
            color = CardBackgroundDark,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(Modifier.widthIn(max = 104.dp)) {
                    Text(title, style = MaterialTheme.typography.labelMedium, color = TextPrimary, maxLines = 1)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (target != null) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(target.provider, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(target.account, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("Auto-sync contests") },
                trailingIcon = { Switch(checked = isAutoSyncEnabled, onCheckedChange = null) },
                onClick = { onAutoSyncChange(!isAutoSyncEnabled) }
            )
            DropdownMenuItem(
                text = { Text("Sync now") },
                onClick = {
                    expanded = false
                    onSyncNow()
                }
            )
        }
    }
}

@Composable
fun PlatformLogo(platform: Platform, size: Int = 40, tile: Color = Color.White) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .background(tile, RoundedCornerShape(10.dp))
            .padding(6.dp)
    ) {
        Image(painter = painterResource(platform.icon), contentDescription = platform.displayName)
    }
}

@Composable
fun ContestCard(
    item: ContestItem,
    now: Long,
    isSynced: Boolean,
    onCalendarClick: (ContestItem) -> Unit
) {
    val context = LocalContext.current
    val platform = item.platform

    val openContest = {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.contest.href)))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        onClick = openContest,
        colors = CardDefaults.cardColors(containerColor = platform.cardColor),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 10.dp)) {
            // Title and platform on the left, platform logo on the right
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.contest.event,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = platform.onCard,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = platform.takeIf { it != Platform.OTHER }?.displayName
                            ?: item.contest.resource.substringBefore("."),
                        style = MaterialTheme.typography.bodySmall,
                        color = platform.onCardMuted
                    )
                }
                Spacer(Modifier.width(12.dp))
                PlatformLogo(platform, tile = Color.White.copy(alpha = 0.6f))
            }

            Spacer(Modifier.height(12.dp))

            // Time left on the left, actions on the right
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (now >= item.startMs) "Live now" else formatCountdown(item.startMs - now),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = platform.onCard
                    )
                    Text(
                        text = "${item.timeText} · ${item.durationText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = platform.onCardMuted
                    )
                }
                IconButton(onClick = { onCalendarClick(item) }) {
                    Icon(
                        imageVector = if (isSynced) Icons.Default.CheckCircle else Icons.Default.DateRange,
                        contentDescription = if (isSynced) "In calendar" else "Calendar sync details",
                        tint = platform.onCardMuted
                    )
                }
                Surface(
                    onClick = openContest,
                    color = platform.onCard.copy(alpha = 0.08f),
                    contentColor = platform.onCard,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Register", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarInfoDialog(
    item: ContestItem,
    hasPermission: Boolean,
    syncedIn: CalendarInfo?,
    target: CalendarInfo?,
    onGrant: () -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    val canAdd = hasPermission && syncedIn == null && target != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackgroundDark,
        title = { Text(item.contest.event, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    !hasPermission -> Text(
                        "Calendar access is off. Allow it so CPCT can save contests and show which calendar they are in.",
                        color = TextSecondary
                    )
                    syncedIn != null -> {
                        Text("Saved in your calendar", color = TextSecondary)
                        CalendarDetails(syncedIn)
                    }
                    target != null -> {
                        Text("Not in your calendar yet. It will be added to:", color = TextSecondary)
                        CalendarDetails(target)
                    }
                    else -> Text(
                        "No writable calendar found. Add a Google account or a calendar app on this device.",
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            when {
                !hasPermission -> TextButton(onClick = onGrant) { Text("Allow access") }
                canAdd -> TextButton(onClick = onAdd) { Text("Add to calendar") }
                else -> TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = if (!hasPermission || canAdd) {
            { TextButton(onClick = onDismiss) { Text("Close") } }
        } else null
    )
}

@Composable
private fun CalendarDetails(info: CalendarInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailRow("Calendar app", info.provider)
        if (info.calendarName.isNotBlank() && info.calendarName != info.account) {
            DetailRow("Calendar", info.calendarName)
        }
        DetailRow(if ('@' in info.account) "Email" else "Account", info.account.ifBlank { "—" })
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

private fun formatCountdown(millis: Long): String {
    val minutes = millis / 60_000
    val days = minutes / 1_440
    val hours = (minutes % 1_440) / 60
    val mins = minutes % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins.coerceAtLeast(1)}m"
    }
}
