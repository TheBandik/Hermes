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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


@Composable
fun SessionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(listSessionFiles(context)) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                SessionRow(file = f)
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
private fun SessionRow(file: File) {
    val context = LocalContext.current
    val label = remember(file.name) { prettify(file) }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            when {
                file.name.endsWith(".csv") -> "text/csv"
                else -> "application/json"
            }
        )
    ) { uri ->
        uri?.let {
            saveFileToUri(context, file, it)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(file.name, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = {
                    saveLauncher.launch(file.name)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Save to device")
            }

            OutlinedButton(
                onClick = { shareFile(context, file) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Share / Export")
            }
        }
    }
}


private fun saveFileToUri(context: Context, file: File, uri: Uri) {
    context.contentResolver.openOutputStream(uri)?.use { output ->
        file.inputStream().use { input ->
            input.copyTo(output)
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
