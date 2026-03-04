package com.voiceassistant.util

import com.voiceassistant.data.model.CommandType
import com.voiceassistant.data.model.ReminderCategory
import com.voiceassistant.data.model.VoiceCommand
import java.util.Calendar

/**
 * Parser komend głosowych w języku polskim.
 * Działa w pełni offline – tylko regex i logika.
 */
object VoiceCommandParser {

    fun parse(text: String): VoiceCommand {
        val lower = text.lowercase().trim()
        return when {
            isAddMedicine(lower) -> parseMedicineReminder(text, lower)
            isAddReminder(lower) -> parseReminder(text, lower)
            isAddEvent(lower) -> parseEvent(text, lower)
            isAddNote(lower) -> parseNote(text, lower)
            else -> VoiceCommand(text, CommandType.UNKNOWN)
        }
    }

    // ── Wykrywanie intencji ───────────────────────────────────────────────────
    private fun isAddMedicine(t: String) =
        t.contains(Regex("(leki?|tabletk|lekarstwo|dawkę|dawka|lek )"))

    private fun isAddReminder(t: String) =
        t.contains(Regex("(przypomnij|przypomnienie|ustaw alarm|obudź|zaremind)"))

    private fun isAddEvent(t: String) =
        t.contains(Regex("(spotkanie|wydarzenie|dodaj do kalendarza|zaplanuj|umów)"))

    private fun isAddNote(t: String) =
        t.contains(Regex("(zanotuj|zapisz|notatka|notatkę|zapamiętaj)"))

    // ── Parsery ───────────────────────────────────────────────────────────────
    private fun parseMedicineReminder(raw: String, lower: String): VoiceCommand {
        val timeMillis = extractTime(lower)
        val data = mutableMapOf<String, String>(
            "category" to ReminderCategory.MEDICINE.name,
            "title" to "Czas na leki 💊"
        )
        timeMillis?.let { data["time"] = it.toString() }
        extractMedicineName(lower)?.let { data["medicine"] = it }
        return VoiceCommand(raw, CommandType.SET_MEDICINE_REMINDER, data)
    }

    private fun parseReminder(raw: String, lower: String): VoiceCommand {
        val data = mutableMapOf<String, String>()
        extractTime(lower)?.let { data["time"] = it.toString() }
        extractTitle(lower)?.let { data["title"] = it }
        return VoiceCommand(raw, CommandType.ADD_REMINDER, data)
    }

    private fun parseEvent(raw: String, lower: String): VoiceCommand {
        val data = mutableMapOf<String, String>()
        extractTime(lower)?.let { data["time"] = it.toString() }
        extractTitle(lower)?.let { data["title"] = it }
        extractLocation(lower)?.let { data["location"] = it }
        return VoiceCommand(raw, CommandType.ADD_EVENT, data)
    }

    private fun parseNote(raw: String, lower: String): VoiceCommand {
        // Wszystko po słowie-kluczu to treść notatki
        val content = lower
            .replace(Regex("^(zanotuj|zapisz|notatka|notatkę|zapamiętaj):?\\s*"), "")
            .trim()
        return VoiceCommand(raw, CommandType.ADD_NOTE, mapOf("content" to content))
    }

    // ── Ekstrakcja czasu ──────────────────────────────────────────────────────
    fun extractTime(text: String): Long? {
        val cal = Calendar.getInstance()

        // "o 14:30", "o 8:00", "o 20:00"
        val exactTime = Regex("o (\\d{1,2}):(\\d{2})").find(text)
        if (exactTime != null) {
            val h = exactTime.groupValues[1].toInt()
            val m = exactTime.groupValues[2].toInt()
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, m)
            cal.set(Calendar.SECOND, 0)
            if (cal.timeInMillis < System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        // "o 14", "o ósmej", "o dwunastej"
        val hourOnly = Regex("o (\\d{1,2})(?!:)").find(text)
        if (hourOnly != null) {
            cal.set(Calendar.HOUR_OF_DAY, hourOnly.groupValues[1].toInt())
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            if (cal.timeInMillis < System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        // Względny czas "za 30 minut", "za godzinę"
        val inMinutes = Regex("za (\\d+) minut").find(text)
        if (inMinutes != null) {
            return System.currentTimeMillis() + inMinutes.groupValues[1].toLong() * 60_000
        }

        val inHours = Regex("za (\\d+) godzin").find(text)
        if (inHours != null) {
            return System.currentTimeMillis() + inHours.groupValues[1].toLong() * 3_600_000
        }

        if (text.contains("za godzinę")) {
            return System.currentTimeMillis() + 3_600_000
        }

        // Specjalne słowa
        return when {
            text.contains("rano") -> {
                cal.set(Calendar.HOUR_OF_DAY, 8); cal.set(Calendar.MINUTE, 0)
                if (cal.timeInMillis < System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            text.contains("wieczor") -> {
                cal.set(Calendar.HOUR_OF_DAY, 20); cal.set(Calendar.MINUTE, 0)
                if (cal.timeInMillis < System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            text.contains("południ") -> {
                cal.set(Calendar.HOUR_OF_DAY, 12); cal.set(Calendar.MINUTE, 0)
                if (cal.timeInMillis < System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            else -> null
        }
    }

    private fun extractTitle(text: String): String? {
        val patterns = listOf(
            Regex("(?:przypomnij mi o|przypomnienie o|ustaw|dodaj)\\s+(.+?)(?:\\s+o\\s+\\d|\\s+jutro|\\s+dziś|$)"),
            Regex("(?:przypomnij|przypomnij mi)\\s+(.+)")
        )
        for (p in patterns) {
            val match = p.find(text)
            if (match != null) return match.groupValues[1].trim().capitalize()
        }
        return null
    }

    private fun extractLocation(text: String): String? {
        val match = Regex("(?:w|na|przy|ul\\.)\\s+([A-Za-zżźćńółęąśŻŹĆŃÓŁĘĄŚ]+(?:\\s+[A-Za-zżźćńółęąśŻŹĆŃÓŁĘĄŚ]+)?)").find(text)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun extractMedicineName(text: String): String? {
        val match = Regex("(?:leki?|tabletk[aię])\\s+([A-Za-zżźćńółęąśŻŹĆŃÓŁĘĄŚ]+)").find(text)
        return match?.groupValues?.get(1)?.trim()
    }
}
