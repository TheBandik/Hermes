package com.bandik.mobileapp.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bandik.mobileapp.recording.LiveStats
import com.bandik.mobileapp.recording.RecordingBus
import com.bandik.mobileapp.recording.RecordingService

@Composable
fun RecordingScreen(
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val stats by RecordingBus.stats.collectAsState()

    // auto-exit when recording finished
    LaunchedEffect(stats.isRecording, stats.secondsLeft) {
        if (!stats.isRecording && stats.secondsLeft == 0) {
            onDone()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Recording", style = MaterialTheme.typography.titleLarge)

        Text("Time left: ${stats.secondsLeft}s")
        Text("Events/sec (last 2s): ${"%.1f".format(stats.eventsPerSec)}")
        Text("Unique beacons (last 2s): ${stats.uniqueBeaconsLast2s}")
        Text("Receiving: ${if (stats.isReceiving) "YES" else "NO"}")

        stats.outputFileName?.let { Text("File: $it") }

        stats.lastError?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { stopRecording(context) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("STOP")
        }
    }
}

private fun stopRecording(context: Context) {
    val i = Intent(context, RecordingService::class.java).apply {
        action = RecordingService.ACTION_STOP
    }
    context.startService(i)
}
