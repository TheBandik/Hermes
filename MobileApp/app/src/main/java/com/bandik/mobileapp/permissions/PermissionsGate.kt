package com.bandik.mobileapp.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

@Composable
fun PermissionsGate(
    content: @Composable () -> Unit
) {
    val required = remember { requiredPermissions() }
    var granted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = required.all { result[it] == true }
    }

    LaunchedEffect(Unit) {
        launcher.launch(required)
    }

    if (granted) {
        content()
    } else {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Permissions required", style = MaterialTheme.typography.titleLarge)
            Text("Bluetooth permissions are required for BLE scanning and RSSI logging.")
            Button(
                onClick = { launcher.launch(required) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Grant permissions")
            }
        }
    }
}

private fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
