package com.voiceassistant.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.voiceassistant.data.db.AppDatabase
import com.voiceassistant.data.model.Reminder
import com.voiceassistant.data.model.RepeatType
import com.voiceassistant.receiver.AlarmReceiver
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ReminderRepository(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).reminderDao()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val allReminders: Flow<List<Reminder>> = dao.getAllReminders()

    suspend fun addReminder(reminder: Reminder): Int {
        val id = dao.insertReminder(reminder).toInt()
        val saved = reminder.copy(id = id)
        if (saved.isActive) scheduleAlarm(saved)
        return id
    }

    suspend fun updateReminder(reminder: Reminder) {
        dao.updateReminder(reminder)
        cancelAlarm(reminder.id)
        if (reminder.isActive) scheduleAlarm(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        cancelAlarm(reminder.id)
        dao.deleteReminder(reminder)
    }

    suspend fun toggleReminder(id: Int, active: Boolean) {
        val reminder = dao.getReminderById(id) ?: return
        val updated = reminder.copy(isActive = active)
        dao.updateReminder(updated)
        if (active) {
            // Dla codziennych – ustaw czas na dziś lub jutro
            val scheduled = if (updated.repeatType == RepeatType.DAILY) {
                nextDailyTime(updated)
            } else updated
            scheduleAlarm(scheduled)
        } else {
            cancelAlarm(id)
        }
    }

    // ── Zaplanuj alarm ────────────────────────────────────────────────────────
    fun scheduleAlarm(reminder: Reminder) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REMINDER
            putExtra("reminder_id", reminder.id)
            putExtra("reminder_title", reminder.title)
            putExtra("reminder_category", reminder.category.name)
            putExtra("sound_enabled", reminder.soundEnabled)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, reminder.id, intent, flags)

        val triggerTime = if (reminder.repeatType == RepeatType.DAILY) {
            nextDailyTime(reminder).dateTime
        } else {
            reminder.dateTime
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()) {
                // Fallback do nieexact jeśli brak uprawnienia
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Wylicz następny czas dla codziennego ─────────────────────────────────
    private fun nextDailyTime(reminder: Reminder): Reminder {
        val cal = Calendar.getInstance()
        val reminderCal = Calendar.getInstance().apply { timeInMillis = reminder.dateTime }
        cal.set(Calendar.HOUR_OF_DAY, reminderCal.get(Calendar.HOUR_OF_DAY))
        cal.set(Calendar.MINUTE, reminderCal.get(Calendar.MINUTE))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // Jeśli czas minął dziś – przesuń na jutro
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return reminder.copy(dateTime = cal.timeInMillis)
    }

    // ── Zaplanuj następne powtórzenie ─────────────────────────────────────────
    fun scheduleNextRepeat(reminderId: Int, currentTime: Long, repeatType: RepeatType) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTime }
        when (repeatType) {
            RepeatType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RepeatType.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatType.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RepeatType.NONE -> return
        }
        // Pobierz reminder z bazy i zaplanuj z nowym czasem
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REMINDER
            putExtra("reminder_id", reminderId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, reminderId, intent, flags)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Anuluj alarm ──────────────────────────────────────────────────────────
    fun cancelAlarm(reminderId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, reminderId, intent, flags)
        alarmManager.cancel(pendingIntent)
    }

    // ── Reschedule po restarcie telefonu ──────────────────────────────────────
    suspend fun rescheduleAllActiveReminders() {
        dao.getActiveReminders().forEach { reminder ->
            if (reminder.isActive) {
                val toSchedule = if (reminder.repeatType == RepeatType.DAILY) {
                    nextDailyTime(reminder)
                } else {
                    // Jednorazowe które minęły – pomiń
                    if (reminder.dateTime < System.currentTimeMillis()) return@forEach
                    reminder
                }
                scheduleAlarm(toSchedule)
            }
        }
    }
}
