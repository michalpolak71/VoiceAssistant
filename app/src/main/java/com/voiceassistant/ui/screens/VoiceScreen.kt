package com.voiceassistant.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voiceassistant.ui.MainViewModel
import com.voiceassistant.ui.theme.Primary

@Composable
fun VoiceScreen(
    viewModel: MainViewModel,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    val isListening by viewModel.isListening.collectAsState()
    val transcription by viewModel.transcriptionText.collectAsState()
    val commandResult by viewModel.commandResult.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val todayReminders = reminders.filter {
        val now = System.currentTimeMillis()
        it.dateTime in now..(now + 86400000)
    }

    // Animacja pulsowania gdy nasłuchuje
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = if (isListening)
            infiniteRepeatable(tween(600), RepeatMode.Reverse)
        else tween(200),
        label = "mic_scale"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            "Asystent Głosowy",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            "Mów po naciśnięciu przycisku",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(40.dp))

        // ── Główny przycisk mikrofonu ─────────────────────────────────────────
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isListening) {
                // Obwódka pulsująca
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .background(Primary.copy(alpha = 0.2f), CircleShape)
                )
            }
            FloatingActionButton(
                onClick = { if (isListening) onStopListening() else onStartListening() },
                modifier = Modifier.size(88.dp),
                containerColor = if (isListening) Color(0xFFFF4444) else Primary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isListening) "Zatrzymaj" else "Nagraj",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            if (isListening) "Słucham... mów teraz" else "Naciśnij i mów",
            fontSize = 16.sp,
            color = if (isListening) Primary else MaterialTheme.colorScheme.onSurface.copy(0.5f),
            fontWeight = if (isListening) FontWeight.Medium else FontWeight.Normal
        )

        // ── Transkrypcja na żywo ─────────────────────────────────────────────
        if (transcription.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "\"$transcription\"",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Wynik komendy ────────────────────────────────────────────────────
        if (commandResult.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Primary.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(commandResult, modifier = Modifier.weight(1f), fontSize = 15.sp)
                    IconButton(onClick = { viewModel.clearCommandResult() }) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Dziś ─────────────────────────────────────────────────────────────
        if (todayReminders.isNotEmpty()) {
            Text(
                "Dzisiaj jeszcze:",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            todayReminders.take(3).forEach { r ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(r.category.emoji, fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(r.title, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(r.dateTime)),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Przykładowe komendy ──────────────────────────────────────────────
        Text(
            "Przykłady komend:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(4.dp))
        listOf(
            "\"Przypomnij mi wziąć leki o 8\"",
            "\"Zanotuj że mam spotkanie w piątek\"",
            "\"Dodaj do kalendarza dentystę o 15:30\""
        ).forEach { cmd ->
            Text(
                cmd,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                textAlign = TextAlign.Center
            )
        }
    }
}
