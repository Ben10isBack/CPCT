package com.example.cpcontesttracker.utils

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.example.cpcontesttracker.ui.ContestItem
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
     * Looks up, in ONE query, which of the given contests already exist in any calendar.
     * Matches on exact start time and exact title. Returns contest id -> calendar holding it.
     */
    fun findSyncedContests(items: List<ContestItem>): Map<Int, CalendarInfo> {
        if (items.isEmpty()) return emptyMap()

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.ACCOUNT_NAME,
            CalendarContract.Events.ACCOUNT_TYPE
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND " +
            "${CalendarContract.Events.DELETED} = 0"
        val args = arrayOf(items.minOf { it.startMs }.toString(), items.maxOf { it.startMs }.toString())

        val found = HashMap<String, CalendarInfo>()
        try {
            resolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, args, null)?.use {
                while (it.moveToNext()) {
                    val key = eventKey(it.getLong(1), it.getString(0).orEmpty())
                    found[key] = CalendarInfo(
                        id = it.getLong(2),
                        provider = providerName(it.getString(5).orEmpty()),
                        calendarName = it.getString(3).orEmpty(),
                        account = it.getString(4).orEmpty()
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied to read events", e)
            return emptyMap()
        }

        val result = HashMap<Int, CalendarInfo>()
        for (item in items) {
            found[eventKey(item.startMs, item.contest.event)]?.let { result[item.contest.id] = it }
        }
        return result
    }

    /** Inserts the given contests into [calendar]. Returns how many were added. */
    fun insertContests(items: List<ContestItem>, calendar: CalendarInfo): Int =
        items.count { insertContest(it, calendar.id) }

    private fun insertContest(item: ContestItem, calendarId: Long): Boolean {
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, item.startMs)
                put(CalendarContract.Events.DTEND, item.endMs)
                put(CalendarContract.Events.TITLE, item.contest.event)
                put(CalendarContract.Events.DESCRIPTION, "Contest URL: ${item.contest.href}")
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
    }
}
