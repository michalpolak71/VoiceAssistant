package com.voiceassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceassistant.data.model.*
import com.voiceassistant.ui.MainViewModel
import com.voiceassistant.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: MainViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Przypomnienia",
                fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.size(48.dp),
                containerColor = Primary
            ) {
                Icon(Icons.Default.Add, "Dodaj", tint = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Szybkie przyciski – leki
        QuickMedicineButtons(onAdd = { viewModel.addReminder(it) })

        Spacer(Modifier.height(12.dp))

        if (reminders.isEmpty()) {
            EmptyReminders()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onToggle = { viewModel.toggleReminder(reminder.id, it) },
                        onDelete = { viewModel.deleteReminder(reminder) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { viewModel.addReminder(it); showAddDialog = false }
        )
    }
}

@Composable
fun QuickMedicineButtons(onAdd: (Reminder) -> Unit) {
    Text(
        "Szybkie dodawanie leków:",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("8:00", "12:00", "20:00").forEach { time ->
            val parts = time.split(":")
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                        set(Calendar.MINUTE, parts[1].toInt())
                        set(Calendar.SECOND, 0)
                        if (timeInMillis < System.currentTimeMillis())
                            add(Calendar.DAY_OF_YEAR, 1)
                    }
                    onAdd(Reminder(
                        title = "Czas na leki 💊",
                        dateTime = cal.timeInMillis,
                        category = ReminderCategory.MEDICINE,
                        repeatType = RepeatType.DAILY
                    ))
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MedicineColor
                )
            ) {
                Icon(Icons.Default.Medication, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(time, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val fmt = SimpleDateFormat("EEE, d MMM  HH:mm", Locale("pl"))
    val catColor = when (reminder.category) {
        ReminderCategory.MEDICINE -> MedicineColor
        ReminderCategory.MEETING -> MeetingColor
        ReminderCategory.TASK -> TaskColor
        else -> GeneralColor
    }
    val isPast = reminder.dateTime < System.currentTimeMillis()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast)
                SurfaceVariant.copy(alpha = 0.5f)
            else SurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji kategorii
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(reminder.category.emoji, fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminder.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isPast)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    fmt.format(Date(reminder.dateTime)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (reminder.repeatType != RepeatType.NONE) {
                    Text(
                        "🔄 ${reminder.repeatType.label}",
                        fontSize = 11.sp,
                        color = catColor.copy(alpha = 0.8f)
                    )
                }
            }

            Switch(
                checked = reminder.isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = catColor)
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete, "Usuń",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

private val RepeatType.label get() = when (this) {
    RepeatType.DAILY -> "Codziennie"
    RepeatType.WEEKLY -> "Co tydzień"
    RepeatType.MONTHLY -> "Co miesiąc"
    RepeatType.NONE -> ""
}

@Composable
fun EmptyReminders() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔔", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Brak przypomnień",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                "Dodaj ręcznie lub powiedz:\n\"Przypomnij mi wziąć leki o 8\"",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (Reminder) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ReminderCategory.GENERAL) }
    var selectedRepeat by remember { mutableStateOf(RepeatType.NONE) }
    var soundEnabled by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Nowe przypomnienie", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł *") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Kategorie
                Text("Kategoria", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReminderCategory.values().take(4).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.emoji} ${cat.label}", fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("Godzina (np. 08:00)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Powtarzanie
                Text("Powtarzaj", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(RepeatType.NONE to "Raz", RepeatType.DAILY to "Codziennie",
                        RepeatType.WEEKLY to "Co tydzień").forEach { (type, label) ->
                        FilterChip(
                            selected = selectedRepeat == type,
                            onClick = { selectedRepeat = type },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dźwięk", modifier = Modifier.weight(1f))
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val cal = Calendar.getInstance()
                        if (timeText.contains(":")) {
                            val parts = timeText.split(":")
                            runCatching {
                                cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                                cal.set(Calendar.MINUTE, parts[1].toInt())
                                cal.set(Calendar.SECOND, 0)
                                if (cal.timeInMillis < System.currentTimeMillis())
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                            }
                        } else {
                            cal.add(Calendar.HOUR_OF_DAY, 1)
                        }
                        onConfirm(Reminder(
                            title = title,
                            dateTime = cal.timeInMillis,
                            category = selectedCategory,
                            repeatType = selectedRepeat,
                            soundEnabled = soundEnabled
                        ))
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Dodaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}
