package com.bandik.mobileapp.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class BleStatus(
    val hasBle: Boolean,
    val hasBluetooth: Boolean,
    val bluetoothEnabled: Boolean
)

@Composable
fun BlePreflightGate(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val status by remember { mutableStateOf(getBleStatus(context)) }

    // Re-check when coming back from Settings (simple approach: recomposition on resume later; for now provide manual refresh)
    var current by remember { mutableStateOf(status) }

    if (current.hasBle && current.hasBluetooth && current.bluetoothEnabled) {
        content()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Bluetooth check", style = MaterialTheme.typography.titleLarge)

        StatusRow("BLE supported", current.hasBle)
        StatusRow("Bluetooth supported", current.hasBluetooth)
        StatusRow("Bluetooth enabled", current.bluetoothEnabled)

        if (!current.hasBle || !current.hasBluetooth) {
            Text(
                "This device does not support Bluetooth Low Energy.",
                color = MaterialTheme.colorScheme.error
            )
        } else if (!current.bluetoothEnabled) {
            Text(
                "Bluetooth is turned off. Turn it on to start scanning.",
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                enabled = current.hasBluetooth,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Open Bluetooth settings")
            }

            OutlinedButton(
                onClick = { current = getBleStatus(context) },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(if (ok) "OK" else "NO")
    }
}

private fun getBleStatus(context: Context): BleStatus {
    val pm = context.packageManager
    val hasBle = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    val hasBluetooth = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter: BluetoothAdapter? = manager.adapter
    val enabled = adapter?.isEnabled == true

    return BleStatus(
        hasBle = hasBle,
        hasBluetooth = hasBluetooth,
        bluetoothEnabled = enabled
    )
}
