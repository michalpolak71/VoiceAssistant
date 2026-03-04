package com.voiceassistant.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voiceassistant.data.db.AppDatabase
import com.voiceassistant.data.model.*
import com.voiceassistant.data.repository.CalendarRepository
import com.voiceassistant.data.repository.ReminderRepository
import com.voiceassistant.service.RecordingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = AppDatabase.getInstance(context)
    private val reminderRepo = ReminderRepository(context)
    private val calendarRepo = CalendarRepository(context)

    // ── State flows ───────────────────────────────────────────────────────────
    val reminders: StateFlow<List<Reminder>> = reminderRepo.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = db.noteDao().getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents

    private val _transcriptionText = MutableStateFlow("")
    val transcriptionText: StateFlow<String> = _transcriptionText

    private val _commandResult = MutableStateFlow("")
    val commandResult: StateFlow<String> = _commandResult

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    // ── Broadcast receiver dla transkrypcji ───────────────────────────────────
    private val transcriptionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                RecordingService.BROADCAST_TRANSCRIPTION -> {
                    _transcriptionText.value = intent.getStringExtra(RecordingService.EXTRA_TEXT) ?: ""
                }
                RecordingService.BROADCAST_COMMAND_RESULT -> {
                    _commandResult.value = intent.getStringExtra(RecordingService.EXTRA_RESULT) ?: ""
                    _isListening.value = false
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(RecordingService.BROADCAST_TRANSCRIPTION)
            addAction(RecordingService.BROADCAST_COMMAND_RESULT)
        }
        context.registerReceiver(transcriptionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        loadCalendarEvents()
    }

    override fun onCleared() {
        context.unregisterReceiver(transcriptionReceiver)
        super.onCleared()
    }

    // ── Akcje ─────────────────────────────────────────────────────────────────
    fun selectTab(index: Int) { _selectedTab.value = index }

    fun loadCalendarEvents() = viewModelScope.launch {
        runCatching {
            _calendarEvents.value = calendarRepo.getWeekEvents()
        }
    }

    fun addReminder(reminder: Reminder) = viewModelScope.launch {
        reminderRepo.addReminder(reminder)
    }

    fun deleteReminder(reminder: Reminder) = viewModelScope.launch {
        reminderRepo.deleteReminder(reminder)
    }

    fun toggleReminder(id: Int, active: Boolean) = viewModelScope.launch {
        reminderRepo.toggleReminder(id, active)
    }

    fun addNote(content: String, title: String = "", linkedEventId: Long? = null) =
        viewModelScope.launch {
            db.noteDao().insertNote(
                Note(
                    title = title,
                    content = content,
                    linkedEventId = linkedEventId,
                    transcribedFrom = false
                )
            )
        }

    fun deleteNote(note: Note) = viewModelScope.launch {
        db.noteDao().deleteNote(note)
    }

    fun addCalendarEvent(
        title: String,
        description: String,
        startMillis: Long,
        endMillis: Long,
        location: String = ""
    ) = viewModelScope.launch {
        val calendars = calendarRepo.getCalendars()
        val calId = calendars.firstOrNull()?.first ?: return@launch
        calendarRepo.addEvent(calId, title, description, startMillis, endMillis, location)
        loadCalendarEvents()
    }

    fun clearCommandResult() { _commandResult.value = "" }
    fun clearTranscription() { _transcriptionText.value = "" }
}
