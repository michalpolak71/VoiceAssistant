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
            isDeleteNote(lower) -> VoiceCommand(text, CommandType.DELETE_NOTE,
                mapOf("query" to extractDeleteTarget(lower, "notatk")))
            isDeleteReminder(lower) -> VoiceCommand(text, CommandType.DELETE_REMINDER,
                mapOf("query" to extractDeleteTarget(lower, "przypomnieni|alarm|lek")))
            isAddMedicine(lower) -> parseMedicineReminder(text, lower)
            isAddReminder(lower) -> parseReminder(text, lower)
            isAddEvent(lower) -> parseEvent(text, lower)
            isAddNote(lower) -> parseNote(text, lower)
            else -> VoiceCommand(text, CommandType.UNKNOWN)
        }
    }

    // ── Wykrywanie intencji ───────────────────────────────────────────────────
    private fun isDeleteNote(t: String) =
        t.contains(Regex("(usu[nń]|skasuj|wykasuj|zlikwiduj).{0,15}(notatk|zapisek)")) ||
        t.contains(Regex("(notatk).{0,15}(usu[nń]|skasuj)"))

    private fun isDeleteReminder(t: String) =
        t.contains(Regex("(usu[nń]|skasuj|wykasuj).{0,20}(przypomnieni|alarm|leki|lek )")) ||
        t.contains(Regex("(przypomnieni|alarm).{0,15}(usu[nń]|skasuj)"))

    private fun isAddMedicine(t: String) =
        t.contains(Regex("(leki?|tabletk|lekarstwo|dawkę|dawka|lek |medykament|pigułk|pastylk)"))

    private fun isAddReminder(t: String) =
        t.contains(Regex("(przypomnij|przypomnienie|ustaw alarm|obudź|zaremind|nie zapomnij|alert|powiadom)"))

    private fun isAddEvent(t: String) =
        t.contains(Regex("(spotkanie|wydarzenie|dodaj do kalendarza|zaplanuj|umów|wizyta|dentyst|lekarz|konferencj|meeting)"))

    private fun isAddNote(t: String) =
        t.contains(Regex("(zanotuj|zapisz|notatka|notatkę|zapamiętaj|dodaj notatkę|zanotować)"))

    // ── Parsery ───────────────────────────────────────────────────────────────
    private fun parseMedicineReminder(raw: String, lower: String): VoiceCommand {
        val timeMillis = extractTime(lower)
        val data = mutableMapOf(
            "category" to ReminderCategory.MEDICINE.name,
            "title" to "Czas na leki"
        )
        timeMillis?.let { data["time"] = it.toString() }
        extractMedicineName(lower)?.let { data["medicine"] = it }
        return VoiceCommand(raw, CommandType.SET_MEDICINE_REMINDER, data)
    }

    private fun parseReminder(raw: String, lower: String): VoiceCommand {
        val data = mutableMapOf<String, String>()
        extractTime(lower)?.let { data["time"] = it.toString() }
        extractReminderTitle(lower)?.let { data["title"] = it }
        return VoiceCommand(raw, CommandType.ADD_REMINDER, data)
    }

    private fun parseEvent(raw: String, lower: String): VoiceCommand {
        val data = mutableMapOf<String, String>()
        extractTime(lower)?.let { data["time"] = it.toString() }
        extractEventTitle(lower)?.let { data["title"] = it }
        extractLocation(lower)?.let { data["location"] = it }
        return VoiceCommand(raw, CommandType.ADD_EVENT, data)
    }

    private fun parseNote(raw: String, lower: String): VoiceCommand {
        val content = lower
            .replace(Regex("^(zanotuj|zapisz|notatka|notatkę|zapamiętaj|dodaj notatkę|zanotować):?\\s*"), "")
            .trim()
        return VoiceCommand(raw, CommandType.ADD_NOTE, mapOf("content" to content.ifEmpty { raw }))
    }

    private fun extractDeleteTarget(text: String, typeRegex: String): String {
        val match = Regex("(usu[nń]|skasuj).{0,10}($typeRegex).{0,5}(.+)").find(text)
        return match?.groupValues?.get(3)?.trim() ?: ""
    }

    // ── Ekstrakcja czasu ──────────────────────────────────────────────────────
    fun extractTime(text: String): Long? {
        val cal = Calendar.getInstance()

        // Jutro
        val isTomorrow = text.contains(Regex("jutro|następny dzień|następnego dnia"))
        if (isTomorrow) cal.add(Calendar.DAY_OF_YEAR, 1)

        // Pojutrze
        if (text.contains("pojutrze")) cal.add(Calendar.DAY_OF_YEAR, 2)

        // Dni tygodnia
        val dayMap = mapOf(
            "poniedział" to Calendar.MONDAY,
            "wtorek" to Calendar.TUESDAY,
            "środa" to Calendar.WEDNESDAY,
            "środę" to Calendar.WEDNESDAY,
            "czwartek" to Calendar.THURSDAY,
            "piątek" to Calendar.FRIDAY,
            "sobota" to Calendar.SATURDAY,
            "sobotę" to Calendar.SATURDAY,
            "niedziela" to Calendar.SUNDAY,
            "niedzielę" to Calendar.SUNDAY
        )
        for ((day, dayConst) in dayMap) {
            if (text.contains(day)) {
                val today = cal.get(Calendar.DAY_OF_WEEK)
                var diff = dayConst - today
                if (diff <= 0) diff += 7
                cal.add(Calendar.DAY_OF_YEAR, diff)
                break
            }
        }

        // "o 14:30", "o 8:00"
        val exactTime = Regex("o (\\d{1,2}):(\\d{2})").find(text)
        if (exactTime != null) {
            cal.set(Calendar.HOUR_OF_DAY, exactTime.groupValues[1].toInt())
            cal.set(Calendar.MINUTE, exactTime.groupValues[2].toInt())
            cal.set(Calendar.SECOND, 0)
            if (!isTomorrow && cal.timeInMillis < System.currentTimeMillis())
                cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        // "o 14", "o 8"
        val hourOnly = Regex("o (\\d{1,2})(?!:)(?= |$)").find(text)
        if (hourOnly != null) {
            cal.set(Calendar.HOUR_OF_DAY, hourOnly.groupValues[1].toInt())
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            if (!isTomorrow && cal.timeInMillis < System.currentTimeMillis())
                cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        // "za 30 minut"
        val inMinutes = Regex("za (\\d+) minut").find(text)
        if (inMinutes != null) {
            return System.currentTimeMillis() + inMinutes.groupValues[1].toLong() * 60_000
        }

        // "za 2 godziny"
        val inHours = Regex("za (\\d+) godzin").find(text)
        if (inHours != null) {
            return System.currentTimeMillis() + inHours.groupValues[1].toLong() * 3_600_000
        }

        if (text.contains("za godzinę") || text.contains("za godzine")) {
            return System.currentTimeMillis() + 3_600_000
        }

        // Pory dnia
        return when {
            text.contains(Regex("rano|poranku|rano|świt")) -> {
                cal.set(Calendar.HOUR_OF_DAY, 8); cal.set(Calendar.MINUTE, 0)
                if (!isTomorrow && cal.timeInMillis < System.currentTimeMillis())
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            text.contains(Regex("wieczor|wieczorem|wieczór")) -> {
                cal.set(Calendar.HOUR_OF_DAY, 20); cal.set(Calendar.MINUTE, 0)
                if (!isTomorrow && cal.timeInMillis < System.currentTimeMillis())
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            text.contains(Regex("południ|południe|w poludnie")) -> {
                cal.set(Calendar.HOUR_OF_DAY, 12); cal.set(Calendar.MINUTE, 0)
                if (!isTomorrow && cal.timeInMillis < System.currentTimeMillis())
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            text.contains(Regex("po południu|popołudniu")) -> {
                cal.set(Calendar.HOUR_OF_DAY, 15); cal.set(Calendar.MINUTE, 0)
                if (!isTomorrow && cal.timeInMillis < System.currentTimeMillis())
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            text.contains(Regex("w nocy|nocą|północ")) -> {
                cal.set(Calendar.HOUR_OF_DAY, 22); cal.set(Calendar.MINUTE, 0)
                if (!isTomorrow && cal.timeInMillis < System.currentTimeMillis())
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            isTomorrow -> cal.timeInMillis
            else -> null
        }
    }

    private fun extractReminderTitle(text: String): String? {
        val patterns = listOf(
            Regex("(?:przypomnij mi [oż]|przypomnienie [oż]|nie zapomnij [oż])\\s+(.+?)(?:\\s+o\\s+\\d|\\s+jutro|\\s+dziś|\\s+za |$)"),
            Regex("(?:przypomnij|powiadom).{0,5}mi\\s+(.+?)(?:\\s+o\\s+\\d|$)"),
            Regex("(?:przypomnij)\\s+(.+)")
        )
        for (p in patterns) {
            val match = p.find(text)
            if (match != null) {
                val title = match.groupValues[1].trim()
                if (title.isNotBlank() && title.length > 2) return title.capitalize()
            }
        }
        return null
    }

    private fun extractEventTitle(text: String): String? {
        val patterns = listOf(
            Regex("(spotkanie|wizyta|konferencja|dentyst\\w+|lekarz)\\s+(.+?)(?:\\s+o\\s+\\d|\\s+jutro|\\s+w \\w+dziel|$)"),
            Regex("(?:dodaj do kalendarza|zaplanuj|umów)\\s+(.+?)(?:\\s+o\\s+\\d|$)")
        )
        for (p in patterns) {
            val match = p.find(text)
            if (match != null) {
                val g = if (match.groupValues.size > 2 && match.groupValues[2].isNotBlank())
                    match.groupValues[2] else match.groupValues[1]
                if (g.isNotBlank()) return g.trim().capitalize()
            }
        }
        // Fallback – pierwsze słowa
        return text.split(" ").take(3).joinToString(" ").capitalize()
    }

    private fun extractLocation(text: String): String? {
        val match = Regex("(?:w |na |przy |ul\\.|ulicy )([A-Za-zżźćńółęąśŻŹĆŃÓŁĘĄŚ]+(?:\\s+[A-Za-zżźćńółęąśŻŹĆŃÓŁĘĄŚ]+)?)").find(text)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun extractMedicineName(text: String): String? {
        val match = Regex("(?:leki?|tabletk[aię]|lek )\\s+([A-Za-zżźćńółęąśŻŹĆŃÓŁĘĄŚ]+)").find(text)
        return match?.groupValues?.get(1)?.trim()
    }
}
