package com.bandik.mobileapp.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SessionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(listSessionFiles(context)) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Sessions", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { files = listSessionFiles(context) }) {
                Text("Refresh")
            }
        }

        if (files.isEmpty()) {
            Text("No session files yet.")
        } else {
            files.forEach { f ->
                SessionRow(
                    file = f,
                    onShare = { shareFile(context, f) }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun SessionRow(file: File, onShare: () -> Unit) {
    val label = remember(file.name) { prettify(file) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(file.name, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Share / Export")
            }
        }
    }
}

private fun listSessionFiles(context: Context): List<File> {
    val dir = File(context.filesDir, "sessions")
    if (!dir.exists()) return emptyList()
    return dir.listFiles()
        ?.filter { it.isFile && (it.name.endsWith(".jsonl") || it.name.endsWith(".csv")) }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()
}

private fun prettify(file: File): String {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    return "Modified: ${df.format(Date(file.lastModified()))} • Size: ${file.length()} bytes"
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val share = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(share, "Export session file"))
}
