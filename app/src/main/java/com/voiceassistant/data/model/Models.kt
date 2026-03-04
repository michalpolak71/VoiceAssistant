package com.voiceassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── REMINDER ────────────────────────────────────────────────────────────────
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val dateTime: Long,
    val repeatType: RepeatType = RepeatType.NONE,
    val isActive: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val category: ReminderCategory = ReminderCategory.GENERAL,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RepeatType {
    NONE, DAILY, WEEKLY, MONTHLY
}

enum class ReminderCategory(val label: String, val emoji: String) {
    MEDICINE("Leki", "💊"),
    MEETING("Spotkanie", "🤝"),
    TASK("Zadanie", "✅"),
    GENERAL("Ogolne", "🔔"),
    WORKOUT("Cwiczenia", "🏃"),
    MEAL("Posilek", "🍽️")
}

// ─── NOTE ─────────────────────────────────────────────────────────────────────
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val content: String,
    val transcribedFrom: Boolean = false,
    val linkedEventId: Long? = null,
    val linkedReminderId: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tags: String = ""
)

// ─── CALENDAR EVENT ──────────────────────────────────────────────────────────
data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean = false,
    val calendarId: Long,
    val location: String = ""
)

// ─── VOICE COMMAND ────────────────────────────────────────────────────────────
data class VoiceCommand(
    val raw: String,
    val type: CommandType,
    val extractedData: Map<String, String> = emptyMap()
)

enum class CommandType {
    ADD_REMINDER,
    ADD_EVENT,
    ADD_NOTE,
    SET_MEDICINE_REMINDER,
    DELETE_NOTE,
    DELETE_REMINDER,
    UNKNOWN
}

// ─── RECORDING ────────────────────────────────────────────────────────────────
@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val transcription: String = "",
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
