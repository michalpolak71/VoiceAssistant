package com.voiceassistant.data.repository

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.voiceassistant.data.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

class CalendarRepository(private val context: Context) {

    // ── Pobierz listę kalendarzy ─────────────────────────────────────────────
    suspend fun getCalendars(): List<Pair<Long, String>> = withContext(Dispatchers.IO) {
        val calendars = mutableListOf<Pair<Long, String>>()
        try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            )
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection, null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1) ?: "Kalendarz"
                    calendars.add(Pair(id, name))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        calendars
    }

    // ── Pobierz wydarzenia z zakresu dat ─────────────────────────────────────
    suspend fun getEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            val events = mutableListOf<CalendarEvent>()
            try {
                val projection = arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DESCRIPTION,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.ALL_DAY,
                    CalendarContract.Events.CALENDAR_ID,
                    CalendarContract.Events.EVENT_LOCATION,
                    CalendarContract.Events.DELETED
                )
                // Pobierz wydarzenia które się zaczynają w tym zakresie LUB trwają przez ten zakres
                val selection = "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?) " +
                        "AND ${CalendarContract.Events.DELETED} = 0"
                val selArgs = arrayOf(startMillis.toString(), endMillis.toString())

                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection, selection, selArgs,
                    "${CalendarContract.Events.DTSTART} ASC"
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(1) ?: continue
                        if (title.isBlank()) continue
                        events.add(
                            CalendarEvent(
                                id = cursor.getLong(0),
                                title = title,
                                description = cursor.getString(2) ?: "",
                                startTime = cursor.getLong(3),
                                endTime = cursor.getLong(4),
                                allDay = cursor.getInt(5) == 1,
                                calendarId = cursor.getLong(6),
                                location = cursor.getString(7) ?: ""
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            events
        }

    // ── Dodaj wydarzenie do kalendarza ───────────────────────────────────────
    suspend fun addEvent(
        calendarId: Long,
        title: String,
        description: String,
        startMillis: Long,
        endMillis: Long,
        location: String = "",
        allDay: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        return@withContext try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                if (location.isNotEmpty()) put(CalendarContract.Events.EVENT_LOCATION, location)
                if (allDay) put(CalendarContract.Events.ALL_DAY, 1)
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLong() ?: -1L
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    // ── Usuń wydarzenie ───────────────────────────────────────────────────────
    suspend fun deleteEvent(eventId: Long) = withContext(Dispatchers.IO) {
        try {
            val uri = CalendarContract.Events.CONTENT_URI
                .buildUpon().appendPath(eventId.toString()).build()
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Pobierz dzisiejsze wydarzenia ─────────────────────────────────────────
    suspend fun getTodayEvents(): List<CalendarEvent> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86400000L
        return getEvents(startOfDay, endOfDay)
    }

    // ── Pobierz wydarzenia z tygodnia ─────────────────────────────────────────
    suspend fun getWeekEvents(): List<CalendarEvent> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val startOfDay = cal.timeInMillis
        return getEvents(startOfDay, startOfDay + 7 * 86400000L)
    }
}
