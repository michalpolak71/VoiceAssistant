package com.voiceassistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.voiceassistant.R
import com.voiceassistant.data.model.CommandType
import com.voiceassistant.data.model.Note
import com.voiceassistant.data.model.Reminder
import com.voiceassistant.data.model.ReminderCategory
import com.voiceassistant.data.repository.ReminderRepository
import com.voiceassistant.util.SpeechRecognitionManager
import com.voiceassistant.util.VoiceCommandParser
import kotlinx.coroutines.*

class RecordingService : Service() {

    companion object {
        const val ACTION_START = "com.voiceassistant.START_RECORDING"
        const val ACTION_STOP = "com.voiceassistant.STOP_RECORDING"
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1002

        // Broadcast do UI
        const val BROADCAST_TRANSCRIPTION = "com.voiceassistant.TRANSCRIPTION"
        const val BROADCAST_COMMAND_RESULT = "com.voiceassistant.COMMAND_RESULT"
        const val EXTRA_TEXT = "text"
        const val EXTRA_RESULT = "result"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var speechManager: SpeechRecognitionManager

    override fun onCreate() {
        super.onCreate()
        speechManager = SpeechRecognitionManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startRecording() {
        startForeground(NOTIFICATION_ID, buildNotification("Nasłuchuję..."))
        scope.launch {
            speechManager.startListening().collect { result ->
                when (result) {
                    is SpeechRecognitionManager.SpeechResult.Partial -> {
                        broadcastTranscription(result.text, partial = true)
                    }
                    is SpeechRecognitionManager.SpeechResult.Final -> {
                        broadcastTranscription(result.text, partial = false)
                        processCommand(result.text)
                        stopSelf()
                    }
                    is SpeechRecognitionManager.SpeechResult.Error -> {
                        broadcastResult("Błąd: ${result.message}")
                        stopSelf()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun stopRecording() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Przetwarzanie komendy głosowej ────────────────────────────────────────
    private suspend fun processCommand(text: String) {
        val command = VoiceCommandParser.parse(text)
        val repo = ReminderRepository(this@RecordingService)

        val resultMsg = when (command.type) {
            CommandType.SET_MEDICINE_REMINDER, CommandType.ADD_REMINDER -> {
                val timeMillis = command.extractedData["time"]?.toLongOrNull()
                    ?: (System.currentTimeMillis() + 3_600_000)
                val title = command.extractedData["title"]
                    ?: command.extractedData["medicine"]
                    ?: "Przypomnienie"
                val category = command.extractedData["category"]
                    ?.let { runCatching { ReminderCategory.valueOf(it) }.getOrNull() }
                    ?: ReminderCategory.GENERAL

                val reminder = Reminder(
                    title = title,
                    dateTime = timeMillis,
                    category = category,
                    soundEnabled = true,
                    vibrationEnabled = true
                )
                withContext(Dispatchers.IO) { repo.addReminder(reminder) }
                "✅ Ustawiono: $title"
            }

            CommandType.ADD_NOTE -> {
                val content = command.extractedData["content"] ?: text
                val db = com.voiceassistant.data.db.AppDatabase.getInstance(this@RecordingService)
                val note = Note(content = content, transcribedFrom = true)
                withContext(Dispatchers.IO) { db.noteDao().insertNote(note) }
                "📝 Zanotowano"
            }

            CommandType.ADD_EVENT -> {
                // Otwieramy MainActivity z danymi do wypełnienia
                val eventIntent = Intent(this@RecordingService,
                    com.voiceassistant.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("add_event_title", command.extractedData["title"] ?: text)
                    putExtra("add_event_time", command.extractedData["time"]?.toLongOrNull() ?: 0L)
                }
                startActivity(eventIntent)
                "📅 Otwieram kalendarz..."
            }

            CommandType.UNKNOWN -> "🤷 Nie rozpoznano komendy. Spróbuj: 'Przypomnij mi o lekach o 8'"
        }

        broadcastResult(resultMsg)
    }

    private fun broadcastTranscription(text: String, partial: Boolean) {
        sendBroadcast(Intent(BROADCAST_TRANSCRIPTION).apply {
            putExtra(EXTRA_TEXT, text)
            putExtra("partial", partial)
        })
    }

    private fun broadcastResult(result: String) {
        sendBroadcast(Intent(BROADCAST_COMMAND_RESULT).apply {
            putExtra(EXTRA_RESULT, result)
        })
    }

    private fun buildNotification(text: String): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Nagrywanie", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Asystent słucha")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mic_active)
            .setOngoing(true)
            .build()
    }
}
