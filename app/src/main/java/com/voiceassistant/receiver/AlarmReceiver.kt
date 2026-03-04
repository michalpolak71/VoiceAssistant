package com.voiceassistant.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.voiceassistant.MainActivity
import com.voiceassistant.R
import com.voiceassistant.data.db.AppDatabase
import com.voiceassistant.data.model.RepeatType
import com.voiceassistant.data.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "com.voiceassistant.REMINDER_ALARM"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_VIBRATION = "vibration"
        const val CHANNEL_ID = "reminders_channel"
        const val CHANNEL_MEDICINE_ID = "medicine_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMINDER -> handleReminder(context, intent)
            Intent.ACTION_BOOT_COMPLETED -> rescheduleAll(context)
        }
    }

    private fun handleReminder(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("reminder_id", -1)
        val title = intent.getStringExtra("reminder_title") ?: "Przypomnienie"
        val soundEnabled = intent.getBooleanExtra("sound_enabled", true)

        showNotification(context, reminderId, title, "", soundEnabled)
        vibrate(context)

        // Zaplanuj nastepne powtorzenie jesli codzienne
        if (reminderId >= 0) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = AppDatabase.getInstance(context).reminderDao()
                val reminder = dao.getReminderById(reminderId) ?: return@launch
                if (reminder.repeatType != RepeatType.NONE) {
                    val repo = ReminderRepository(context)
                    repo.scheduleNextRepeat(reminderId, reminder.dateTime, reminder.repeatType)
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        description: String,
        soundEnabled: Boolean
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(nm)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_reminders", true)
        }
        val tapPending = PendingIntent.getActivity(
            context, id, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = if (soundEnabled)
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        else null

        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(description.ifEmpty { "Dotknij aby zobaczyc szczegoly" })
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(tapPending, true)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .apply { soundUri?.let { setSound(it) } }
            .build()

        nm.notify(id, notification)
    }

    private fun vibrate(context: Context) {
        val pattern = longArrayOf(0, 500, 200, 500, 200, 800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, -1)
            }
        }
    }

    private fun createChannels(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Przypomnienia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Powiadomienia o przypomnieniach"
                enableVibration(true)
                val audioAttr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttr)
            }
            nm.createNotificationChannel(channel)

            val medicineChannel = NotificationChannel(
                CHANNEL_MEDICINE_ID, "Leki",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Przypomnienia o lekach"
                enableVibration(true)
            }
            nm.createNotificationChannel(medicineChannel)
        }
    }

    private fun rescheduleAll(context: Context) {
        val repo = ReminderRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            repo.allReminders.collect { list ->
                list.filter { it.isActive }.forEach { reminder ->
                    repo.scheduleAlarm(reminder)
                }
                return@collect
            }
        }
    }
}
