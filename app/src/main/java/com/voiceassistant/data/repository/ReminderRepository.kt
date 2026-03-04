package com.voiceassistant.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.voiceassistant.data.db.AppDatabase
import com.voiceassistant.data.model.Reminder
import com.voiceassistant.data.model.RepeatType
import com.voiceassistant.receiver.AlarmReceiver
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).reminderDao()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val allReminders: Flow<List<Reminder>> = dao.getAllReminders()
    val activeReminders: Flow<List<Reminder>> = dao.getActiveReminders()

    suspend fun addReminder(reminder: Reminder): Int {
        val id = dao.insertReminder(reminder).toInt()
        scheduleAlarm(reminder.copy(id = id))
        return id
    }

    suspend fun updateReminder(reminder: Reminder) {
        cancelAlarm(reminder.id)
        dao.updateReminder(reminder)
        if (reminder.isActive) scheduleAlarm(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        cancelAlarm(reminder.id)
        dao.deleteReminder(reminder)
    }

    suspend fun toggleReminder(id: Int, active: Boolean) {
        dao.setActive(id, active)
        val reminder = dao.getReminderById(id) ?: return
        if (active) scheduleAlarm(reminder) else cancelAlarm(id)
    }

    // ── Ustaw alarm w systemie ────────────────────────────────────────────────
    fun scheduleAlarm(reminder: Reminder) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REMINDER
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, reminder.title)
            putExtra(AlarmReceiver.EXTRA_DESCRIPTION, reminder.description)
            putExtra(AlarmReceiver.EXTRA_SOUND, reminder.soundEnabled)
            putExtra(AlarmReceiver.EXTRA_VIBRATION, reminder.vibrationEnabled)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminder.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.dateTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback dla urządzeń z restrykcjami
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.dateTime, pendingIntent)
        }
    }

    // ── Anuluj alarm ──────────────────────────────────────────────────────────
    private fun cancelAlarm(reminderId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminderId, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    // ── Zaplanuj kolejne powtórzenie ──────────────────────────────────────────
    fun scheduleNextRepeat(reminder: Reminder) {
        val nextTime = when (reminder.repeatType) {
            RepeatType.DAILY -> reminder.dateTime + 86400000L
            RepeatType.WEEKLY -> reminder.dateTime + 7 * 86400000L
            RepeatType.MONTHLY -> reminder.dateTime + 30 * 86400000L
            RepeatType.NONE -> return
        }
        scheduleAlarm(reminder.copy(dateTime = nextTime))
    }
}
