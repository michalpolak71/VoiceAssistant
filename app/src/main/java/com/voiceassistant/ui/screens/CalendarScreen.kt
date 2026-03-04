package com.voiceassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.voiceassistant.data.model.CalendarEvent
import com.voiceassistant.ui.MainViewModel
import com.voiceassistant.ui.theme.Primary
import com.voiceassistant.ui.theme.SurfaceVariant
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: MainViewModel) {
    val events by viewModel.calendarEvents.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Kalendarz",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { viewModel.loadCalendarEvents() }) {
                        Icon(Icons.Default.Refresh, "Odswiez", tint = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.Add, "Dodaj", tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Dodaj", color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Nadchodzace (7 dni)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(12.dp))

            if (events.isEmpty()) {
                EmptyCalendar()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(events) { event ->
                        EventCard(event = event)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, start, end, loc ->
                viewModel.addCalendarEvent(title, desc, start, end, loc)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun EventCard(event: CalendarEvent) {
    val fmt = SimpleDateFormat("EEE, d MMM  HH:mm", Locale("pl"))
    val colors = listOf(
        Color(0xFF6750A4), Color(0xFF03DAC6),
        Color(0xFFFF6B6B), Color(0xFFFFE66D)
    )
    val color = colors[event.id.toInt().mod(colors.size)]

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    fmt.format(Date(event.startTime)),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (event.location.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn, null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            event.location,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCalendar() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CalendarToday, null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Brak wydarzen w tym tygodniu",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                "Dotknij DODAJ aby dodac lub uzyj glosu",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Long, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("") }
    var minute by remember { mutableStateOf("0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Nowe wydarzenie",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytul *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Miejsce") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Data:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("Dzien") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it },
                        label = { Text("Miesiac") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("Godzina:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it },
                        label = { Text("Godz (np. 7)") },
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
                                    if (day.isNotBlank()) cal.set(Calendar.DAY_OF_MONTH, day.toInt())
                                    if (month.isNotBlank()) cal.set(Calendar.MONTH, month.toInt() - 1)
                                    if (hour.isNotBlank()) cal.set(Calendar.HOUR_OF_DAY, hour.toInt())
                                    cal.set(Calendar.MINUTE, minute.toIntOrNull() ?: 0)
                                    cal.set(Calendar.SECOND, 0)
                                }
                                onConfirm(title, description, cal.timeInMillis, cal.timeInMillis + 3600000, location)
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
