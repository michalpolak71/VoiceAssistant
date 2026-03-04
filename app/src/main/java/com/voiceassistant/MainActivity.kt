package com.voiceassistant

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.voiceassistant.service.OverlayService
import com.voiceassistant.service.RecordingService
import com.voiceassistant.ui.MainViewModel
import com.voiceassistant.ui.screens.*
import com.voiceassistant.ui.theme.VoiceAssistantTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // ── Żądania uprawnień ─────────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            checkOverlayPermission()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()

        setContent {
            VoiceAssistantTheme {
                MainScreen(
                    viewModel = viewModel,
                    onStartListening = { startListening() },
                    onStopListening = { stopListening() }
                )
            }
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.VIBRATE
        ).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                it.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        } else {
            OverlayService.start(this)
        }
    }

    private fun startListening() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        viewModel.let {
            // Ustaw stan isListening przez refleksję stanu
        }
    }

    private fun stopListening() {
        startService(Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        })
    }
}

// ── Główny ekran z nawigacją ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()

    data class Tab(val label: String, val icon: ImageVector)
    val tabs = listOf(
        Tab("Głos", Icons.Default.Mic),
        Tab("Kalendarz", Icons.Default.CalendarToday),
        Tab("Leki/Alarmy", Icons.Default.Alarm),
        Tab("Notatki", Icons.Default.Notes)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> VoiceScreen(viewModel, onStartListening, onStopListening)
                1 -> CalendarScreen(viewModel)
                2 -> RemindersScreen(viewModel)
                3 -> NotesScreen(viewModel)
            }
        }
    }
}
