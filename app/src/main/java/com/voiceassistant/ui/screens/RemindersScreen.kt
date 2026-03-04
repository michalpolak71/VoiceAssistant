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
import androidx.compose.ui.window.Dialog
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
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Add, "Dodaj", tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("Dodaj", color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Szybkie przyciski – leki
        QuickMedicineButtons(onAdd = { viewModel.addReminder(it) })

        Spacer(Modifier.height(12.dp))

        if (reminders.isEmpty()) {
            EmptyReminders()
        } else {
            // Grupuj: aktywne codzienne na górze, potem reszta
            val daily = reminders.filter { it.repeatType == RepeatType.DAILY && it.isActive }
            val others = reminders.filter { it.repeatType != RepeatType.DAILY || !it.isActive }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (daily.isNotEmpty()) {
                    item {
                        Text(
                            "Codzienne:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(daily, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggle = { viewModel.toggleReminder(reminder.id, it) },
                            onDelete = { viewModel.deleteReminder(reminder) }
                        )
                    }
                }
                if (others.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Jednorazowe:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(others, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggle = { viewModel.toggleReminder(reminder.id, it) },
                            onDelete = { viewModel.deleteReminder(reminder) }
                        )
                    }
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
        "Szybkie dodawanie lekow:",
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
                        title = "Czas na leki",
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
    // Dla codziennych pokazuj tylko godzinę, dla jednorazowych pełną datę
    val timeText = if (reminder.repeatType == RepeatType.DAILY) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reminder.dateTime))
    } else {
        SimpleDateFormat("EEE, d MMM  HH:mm", Locale("pl")).format(Date(reminder.dateTime))
    }

    val catColor = when (reminder.category) {
        ReminderCategory.MEDICINE -> MedicineColor
        ReminderCategory.MEETING -> MeetingColor
        ReminderCategory.TASK -> TaskColor
        else -> GeneralColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (!reminder.isActive)
                SurfaceVariant.copy(alpha = 0.5f)
            else SurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    timeText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (reminder.repeatType != RepeatType.NONE) {
                    Text(
                        "Powtarza sie: ${reminder.repeatType.label}",
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
                    Icons.Default.Delete, "Usun",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

private val RepeatType.label get() = when (this) {
    RepeatType.DAILY -> "Codziennie"
    RepeatType.WEEKLY -> "Co tydzien"
    RepeatType.MONTHLY -> "Co miesiac"
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
                "Brak przypomnien",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                "Dodaj recznie lub powiedz:\n\"Przypomnij mi wziac leki o 8\"",
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
    var hour by remember { mutableStateOf("") }
    var minute by remember { mutableStateOf("0") }
    var selectedCategory by remember { mutableStateOf(ReminderCategory.GENERAL) }
    var selectedRepeat by remember { mutableStateOf(RepeatType.NONE) }
    var soundEnabled by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Nowe przypomnienie", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytul *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Kategoria:", fontSize = 13.sp,
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

                Text("Godzina:", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it },
                        label = { Text("Godz (np. 8)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it },
                        label = { Text("Min (np. 30)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("Powtarzaj:", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        RepeatType.NONE to "Raz",
                        RepeatType.DAILY to "Codziennie",
                        RepeatType.WEEKLY to "Co tydzien"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = selectedRepeat == type,
                            onClick = { selectedRepeat = type },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dzwiek alarmu", modifier = Modifier.weight(1f))
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Anuluj") }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val cal = Calendar.getInstance()
                                runCatching {
                                    if (hour.isNotBlank()) cal.set(Calendar.HOUR_OF_DAY, hour.toInt())
                                    cal.set(Calendar.MINUTE, minute.toIntOrNull() ?: 0)
                                    cal.set(Calendar.SECOND, 0)
                                    if (cal.timeInMillis < System.currentTimeMillis())
                                        cal.add(Calendar.DAY_OF_YEAR, 1)
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
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank()
                    ) { Text("Dodaj") }
                }
            }
        }
    }
}
