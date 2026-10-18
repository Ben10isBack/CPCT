package com.example.cpcontesttracker.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cpcontesttracker.data.api.RetrofitInstance
import com.example.cpcontesttracker.data.model.Contest
import com.example.cpcontesttracker.utils.CalendarInfo
import com.example.cpcontesttracker.utils.CalendarSyncHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A contest prepared for display: times are parsed and formatted once, off the main thread,
 * so the list never does any parsing while scrolling.
 */
data class ContestItem(
    val contest: Contest,
    val platform: Platform,
    val startMs: Long,
    val endMs: Long,
    val startEpochDay: Long,  // Local calendar day, used to group the list by day
    val dayText: String,      // e.g. "Wednesday, 21 Oct"
    val timeText: String,
    val durationText: String
)

sealed class ContestUiState {
    object Loading : ContestUiState()
    data class Success(
        val contests: List<ContestItem>,
        val isOffline: Boolean = false,
        val isRefreshing: Boolean = false
    ) : ContestUiState()
    data class Error(val message: String) : ContestUiState()
}

data class CalendarState(
    val hasPermission: Boolean = false,
    val target: CalendarInfo? = null,             // Calendar new contests get synced to
    val synced: Map<Int, CalendarInfo> = emptyMap() // Contest id -> calendar it is saved in
)

class ContestViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ContestUiState>(ContestUiState.Loading)
    val uiState: StateFlow<ContestUiState> = _uiState.asStateFlow()

    private val _calendarState = MutableStateFlow(CalendarState())
    val calendarState: StateFlow<CalendarState> = _calendarState.asStateFlow()

    private val cachePrefs = application.getSharedPreferences("contest_cache", Context.MODE_PRIVATE)
    private val settingsPrefs = application.getSharedPreferences("cp_prefs", Context.MODE_PRIVATE)

    private val _autoSyncEnabled = MutableStateFlow(settingsPrefs.getBoolean("auto_sync_enabled", true))
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    // One-off messages for Toasts
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val gson = Gson()
    private val calendar = CalendarSyncHelper(application)

    private var loadedPlatforms: Set<String>? = null
    private var fetchJob: Job? = null
    private val calendarMutex = Mutex()

    /** Loads contests. Does nothing if these platforms are already loaded (e.g. after rotation). */
    fun fetchContests(selectedPlatforms: Set<String>, force: Boolean = false) {
        if (!force && selectedPlatforms == loadedPlatforms && _uiState.value is ContestUiState.Success) return
        loadedPlatforms = selectedPlatforms

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val current = _uiState.value
            if (force && current is ContestUiState.Success) {
                _uiState.value = current.copy(isRefreshing = true)
            } else {
                // 1. Show cached data immediately (parsed off the main thread)
                val cached = withContext(Dispatchers.Default) { readCache()?.let { toItems(it, selectedPlatforms) } }
                if (cached != null) {
                    _uiState.value = ContestUiState.Success(cached, isRefreshing = true)
                    refreshCalendar(autoSync = false)
                } else {
                    _uiState.value = ContestUiState.Loading
                }
            }

            // 2. Fetch fresh data from the network
            try {
                val response = RetrofitInstance.api.getContests()
                val items = withContext(Dispatchers.Default) {
                    cachePrefs.edit().putString(CACHE_KEY, gson.toJson(response.contests)).apply()
                    toItems(response.contests, selectedPlatforms)
                }
                _uiState.value = ContestUiState.Success(items)
                refreshCalendar(autoSync = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching fresh contests", e)
                val state = _uiState.value
                _uiState.value = if (state is ContestUiState.Success) {
                    state.copy(isOffline = true, isRefreshing = false)
                } else {
                    ContestUiState.Error(e.localizedMessage ?: "Unknown error. Check your internet connection.")
                }
            }
        }
    }

    fun refresh() {
        loadedPlatforms?.let { fetchContests(it, force = true) }
    }

    fun onCalendarPermissionResult(granted: Boolean) {
        _calendarState.update { it.copy(hasPermission = granted) }
        if (granted) refreshCalendar(autoSync = true)
    }

    fun setAutoSync(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        settingsPrefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
        if (enabled) refreshCalendar(autoSync = true)
    }

    /** Manually sync every contest that is not in the calendar yet. */
    fun syncNow() = runCalendarJob { items, target, synced ->
        val missing = items.filter { it.contest.id !in synced }
        val added = if (target != null) calendar.insertContests(missing, target) else 0
        _messages.send(
            when {
                target == null -> "No writable calendar found on this device."
                added > 0 -> "Synced $added new contests!"
                else -> "Everything is already synced."
            }
        )
        added > 0
    }

    fun addToCalendar(item: ContestItem) = runCalendarJob { _, target, _ ->
        val added = target != null && calendar.insertContests(listOf(item), target) > 0
        _messages.send(if (added) "Added to ${target?.provider}" else "Could not add contest to calendar.")
        added
    }

    private fun refreshCalendar(autoSync: Boolean) = runCalendarJob { items, target, synced ->
        if (!autoSync || !_autoSyncEnabled.value || target == null) return@runCalendarJob false
        val missing = items.filter { it.contest.id !in synced }
        if (missing.isEmpty()) return@runCalendarJob false
        val added = calendar.insertContests(missing, target)
        if (added > 0) _messages.send("Auto-synced $added new contests!")
        added > 0
    }

    /**
     * Runs calendar work on the IO dispatcher and publishes the fresh calendar state.
     * [block] returns true when it changed the calendar, so synced events get re-read.
     */
    private fun runCalendarJob(
        block: suspend (List<ContestItem>, CalendarInfo?, Map<Int, CalendarInfo>) -> Boolean
    ) {
        if (!_calendarState.value.hasPermission) return

        // Serialized (not cancelled) so a manual sync is never lost and nothing is inserted twice
        viewModelScope.launch(Dispatchers.IO) {
            calendarMutex.withLock {
                runCalendarWork(block)
            }
        }
    }

    private suspend fun runCalendarWork(
        block: suspend (List<ContestItem>, CalendarInfo?, Map<Int, CalendarInfo>) -> Boolean
    ) {
        val items = (_uiState.value as? ContestUiState.Success)?.contests.orEmpty()
        val target = calendar.getTargetCalendar()
        var synced = calendar.findSyncedContests(items)
        _calendarState.update { it.copy(target = target, synced = synced) }

        if (block(items, target, synced)) {
            synced = calendar.findSyncedContests(items)
            _calendarState.update { it.copy(synced = synced) }
        }
    }

    private fun readCache(): List<Contest>? {
        val json = cachePrefs.getString(CACHE_KEY, null) ?: return null
        return try {
            gson.fromJson(json, object : TypeToken<List<Contest>>() {}.type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing cache", e)
            null
        }
    }

    private fun toItems(contests: List<Contest>, selectedPlatforms: Set<String>): List<ContestItem> {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()

        return contests.asSequence()
            .filter { it.resource in selectedPlatforms && !CYRILLIC.containsMatchIn(it.event) }
            .mapNotNull { contest ->
                val start = parseUtcMillis(contest.start) ?: return@mapNotNull null
                val end = parseUtcMillis(contest.end) ?: return@mapNotNull null
                if (end < now) return@mapNotNull null // Already finished

                val startLocal = Instant.ofEpochMilli(start).atZone(zone)
                ContestItem(
                    contest = contest,
                    platform = Platform.from(contest.resource),
                    startMs = start,
                    endMs = end,
                    startEpochDay = startLocal.toLocalDate().toEpochDay(),
                    dayText = DAY_FORMAT.format(startLocal),
                    timeText = TIME_FORMAT.format(startLocal),
                    durationText = formatDuration((end - start) / 1000)
                )
            }
            .sortedBy { it.startMs }
            .toList()
    }

    companion object {
        private const val TAG = "ContestViewModel"
        private const val CACHE_KEY = "cached_contests"
        private val CYRILLIC = Regex("[А-Яа-я]")
        private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

        /** Clist returns UTC times like "2026-10-20T14:35:00" (sometimes with an offset). */
        fun parseUtcMillis(value: String): Long? = try {
            LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (e: Exception) {
            try {
                OffsetDateTime.parse(value).toInstant().toEpochMilli()
            } catch (e: Exception) {
                null
            }
        }

        fun formatDuration(seconds: Long): String {
            val days = seconds / 86_400
            val hours = (seconds % 86_400) / 3_600
            val minutes = (seconds % 3_600) / 60
            return when {
                days > 0 -> if (hours > 0) "${days}d ${hours}h" else "${days}d"
                hours > 0 -> if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
                else -> "${minutes}m"
            }
        }
    }
}
