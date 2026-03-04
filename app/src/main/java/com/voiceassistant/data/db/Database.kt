package com.voiceassistant.data.db

import android.content.Context
import androidx.room.*
import com.voiceassistant.data.model.Note
import com.voiceassistant.data.model.Recording
import com.voiceassistant.data.model.Reminder
import kotlinx.coroutines.flow.Flow

// ─── REMINDER DAO ─────────────────────────────────────────────────────────────
@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY dateTime ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY dateTime ASC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE dateTime >= :from AND dateTime <= :to ORDER BY dateTime ASC")
    fun getRemindersBetween(from: Long, to: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("UPDATE reminders SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Int, active: Boolean)
}

// ─── NOTE DAO ─────────────────────────────────────────────────────────────────
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE linkedEventId = :eventId")
    fun getNotesByEvent(eventId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}

// ─── RECORDING DAO ────────────────────────────────────────────────────────────
@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<Recording>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: Recording): Long

    @Delete
    suspend fun deleteRecording(recording: Recording)
}

// ─── DATABASE ─────────────────────────────────────────────────────────────────
@Database(
    entities = [Reminder::class, Note::class, Recording::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voiceassistant.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
