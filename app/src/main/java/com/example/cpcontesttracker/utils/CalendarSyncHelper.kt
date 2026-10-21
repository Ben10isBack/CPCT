package com.example.cpcontesttracker.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.example.cpcontesttracker.ui.ContestItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

/**
 * Describes a device calendar: which app/provider owns it and which account (email) it belongs to.
 */
data class CalendarInfo(
    val id: Long,
    val provider: String,     // e.g. "Google Calendar", "Samsung Calendar"
    val calendarName: String, // Display name of the calendar itself
    val account: String       // Account name — the email address for Google calendars
)

/** A calendar event as read back from the device, used to match it to a contest. */
private data class SavedEvent(
    val id: Long,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val description: String,
    val location: String,
    val calendar: CalendarInfo
)

/**
 * All methods do blocking ContentResolver work: call them from a background dispatcher.
 */
class CalendarSyncHelper(context: Context) {

    private val resolver = context.applicationContext.contentResolver

    /**
     * Finds the best calendar to sync to.
     * Priority 1: A visible Google Calendar (primary one if possible).
     * Priority 2: Any visible calendar that allows us to write to it (Samsung, Xiaomi, OnePlus, etc).
     */
    fun getTargetCalendar(): CalendarInfo? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY
        )
        // Only get calendars that are visible AND we have permission to edit them
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}"

        return try {
            var google: CalendarInfo? = null
            var fallback: CalendarInfo? = null
            resolver.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null)?.use {
                while (it.moveToNext()) {
                    val accountType = it.getString(3).orEmpty()
                    val info = CalendarInfo(
                        id = it.getLong(0),
                        provider = providerName(accountType),
                        calendarName = it.getString(1).orEmpty(),
                        account = it.getString(2).orEmpty()
                    )
                    val isPrimary = it.getInt(4) == 1
                    if (accountType == GOOGLE_ACCOUNT_TYPE) {
                        if (isPrimary) return info
                        if (google == null) google = info
                    }
                    if (fallback == null) fallback = info
                }
            }
            google ?: fallback
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for reading calendars", e)
            null
        }
    }

    /**
     * Looks up, in ONE query, which of the given contests already exist in any calendar, and brings
     * those events up to date. Returns contest id -> calendar holding it.
     *
     * Events are matched on the contest URL saved in their description, which stays the same when a
     * contest is renamed or rescheduled ("Codeforces Round (Div. 2)" -> "Codeforces Round 1129 (Div. 2)").
     * The matched event gets the current title, times and description, and any extra copies are deleted.
     * Events without a URL (or with a URL shared by several contests) fall back to exact title + start time.
     */
    fun findSyncedContests(items: List<ContestItem>): Map<Int, CalendarInfo> {
        if (items.isEmpty()) return emptyMap()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.ACCOUNT_NAME,
            CalendarContract.Events.ACCOUNT_TYPE
        )
        // Wide window so events of contests that moved to a different date are still found
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND " +
            "${CalendarContract.Events.DELETED} = 0"
        val args = arrayOf(
            (items.minOf { it.startMs } - SEARCH_MARGIN_MS).toString(),
            (items.maxOf { it.startMs } + SEARCH_MARGIN_MS).toString()
        )

        val byUrl = HashMap<String, MutableList<SavedEvent>>()
        val byKey = HashMap<String, SavedEvent>()
        try {
            resolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, args, null)?.use {
                while (it.moveToNext()) {
                    val event = SavedEvent(
                        id = it.getLong(0),
                        title = it.getString(1).orEmpty(),
                        startMs = it.getLong(2),
                        endMs = it.getLong(3),
                        description = it.getString(4).orEmpty(),
                        location = it.getString(5).orEmpty(),
                        calendar = CalendarInfo(
                            id = it.getLong(6),
                            provider = providerName(it.getString(9).orEmpty()),
                            calendarName = it.getString(7).orEmpty(),
                            account = it.getString(8).orEmpty()
                        )
                    )
                    contestUrl(event.description)?.let { url -> byUrl.getOrPut(url) { ArrayList() } += event }
                    byKey[eventKey(event.startMs, event.title)] = event
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied to read events", e)
            return emptyMap()
        }

        // A URL only identifies a contest when no other contest in the list uses it
        val urlCounts = items.groupingBy { it.contest.href }.eachCount()

        val result = HashMap<Int, CalendarInfo>()
        for (item in items) {
            val sameUrl = if (urlCounts[item.contest.href] == 1) byUrl[item.contest.href].orEmpty() else emptyList()
            val exact = byKey[eventKey(item.startMs, item.contest.event)]
            val keep = exact ?: sameUrl.firstOrNull() ?: continue

            sameUrl.filter { it.id != keep.id }.forEach { deleteEvent(it) }
            updateEvent(keep, item)
            result[item.contest.id] = keep.calendar
        }
        return result
    }

    /** Inserts the given contests into [calendar]. Returns how many were added. */
    fun insertContests(items: List<ContestItem>, calendar: CalendarInfo): Int =
        items.count { insertContest(it, calendar.id) }

    /**
     * Shortens events saved before long contests were limited to their first day,
     * so a 14-day contest stops filling two weeks of the calendar. Returns how many changed.
     */
    fun shortenLongContestEvents(items: List<ContestItem>): Int {
        val longContests = items.filter { it.isLong() }
        if (longContests.isEmpty()) return 0
        val byKey = longContests.associateBy { eventKey(it.startMs, it.contest.event) }

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND " +
            "${CalendarContract.Events.DELETED} = 0"
        val args = arrayOf(longContests.minOf { it.startMs }.toString(), longContests.maxOf { it.startMs }.toString())

        var changed = 0
        try {
            resolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, args, null)?.use {
                while (it.moveToNext()) {
                    val item = byKey[eventKey(it.getLong(2), it.getString(1).orEmpty())] ?: continue
                    if (it.getLong(3) == eventEnd(item)) continue

                    val values = ContentValues().apply {
                        put(CalendarContract.Events.DTEND, eventEnd(item))
                        put(CalendarContract.Events.DESCRIPTION, description(item))
                    }
                    val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, it.getLong(0))
                    if (resolver.update(uri, values, null, null) > 0) changed++
                    else Log.w(TAG, "Could not shorten event for ${item.contest.event}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error shortening long contest events", e)
        }
        return changed
    }

    /** Gives an existing event the contest's current title, times, description and location, if they changed. */
    private fun updateEvent(event: SavedEvent, item: ContestItem) {
        val values = ContentValues()
        if (event.title != item.contest.event) values.put(CalendarContract.Events.TITLE, item.contest.event)
        if (event.startMs != item.startMs) values.put(CalendarContract.Events.DTSTART, item.startMs)
        if (event.endMs != eventEnd(item)) values.put(CalendarContract.Events.DTEND, eventEnd(item))
        if (event.description != description(item)) values.put(CalendarContract.Events.DESCRIPTION, description(item))
        if (event.location != item.contest.resource) values.put(CalendarContract.Events.EVENT_LOCATION, item.contest.resource)
        if (values.size() == 0) return

        try {
            resolver.update(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id), values, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event for ${item.contest.event}", e)
        }
    }

    /** Removes a duplicate copy of a contest that an earlier sync added under an old name. */
    private fun deleteEvent(event: SavedEvent) {
        try {
            resolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id), null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting duplicate event ${event.title}", e)
        }
    }

    private fun insertContest(item: ContestItem, calendarId: Long): Boolean {
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, item.startMs)
                put(CalendarContract.Events.DTEND, eventEnd(item))
                put(CalendarContract.Events.TITLE, item.contest.event)
                put(CalendarContract.Events.DESCRIPTION, description(item))
                put(CalendarContract.Events.EVENT_LOCATION, item.contest.resource)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            resolver.insert(CalendarContract.Events.CONTENT_URI, values) != null
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for writing calendar", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting event", e)
            false
        }
    }

    private fun eventKey(startMs: Long, title: String) = "$startMs|$title"

    /** The contest URL from an event description written by [description], or null for other events. */
    private fun contestUrl(description: String): String? =
        description.lineSequence()
            .firstOrNull { it.startsWith(URL_PREFIX) }
            ?.removePrefix(URL_PREFIX)?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun ContestItem.isLong() = endMs - startMs >= LONG_CONTEST_MS

    /** Long contests are saved as a short event on their first day only, not across every day they run. */
    private fun eventEnd(item: ContestItem): Long =
        if (item.isLong()) item.startMs + LONG_CONTEST_EVENT_MS else item.endMs

    private fun description(item: ContestItem): String {
        val url = "$URL_PREFIX${item.contest.href}"
        if (!item.isLong()) return url
        val ends = END_FORMAT.format(Instant.ofEpochMilli(item.endMs).atZone(ZoneId.systemDefault()))
        return "Runs for ${item.durationText}, ends $ends.\n$url"
    }

    private fun providerName(accountType: String): String = when {
        accountType == GOOGLE_ACCOUNT_TYPE -> "Google Calendar"
        accountType.contains("samsung", ignoreCase = true) -> "Samsung Calendar"
        accountType.contains("xiaomi", ignoreCase = true) -> "Xiaomi Calendar"
        accountType.contains("oneplus", ignoreCase = true) -> "OnePlus Calendar"
        accountType.contains("exchange", ignoreCase = true) -> "Exchange / Outlook"
        accountType == CalendarContract.ACCOUNT_TYPE_LOCAL -> "On this device"
        accountType.isNotEmpty() -> accountType
        else -> "System Calendar"
    }

    private companion object {
        const val TAG = "CalendarSync"
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
        const val LONG_CONTEST_MS = 24 * 60 * 60 * 1000L     // Contests lasting a day or more...
        const val LONG_CONTEST_EVENT_MS = 60 * 60 * 1000L    // ...get a 1-hour event at their start
        const val SEARCH_MARGIN_MS = 30 * 24 * 60 * 60 * 1000L // How far a rescheduled contest's old event can be
        const val URL_PREFIX = "Contest URL: "
        val END_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, hh:mm a", Locale.getDefault())
    }
}
