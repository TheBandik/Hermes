package com.bandik.mobileapp.power

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun BatteryOptimizationGate(
    requireDisabled: Boolean = true, // if true => block until disabled
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var ignoring by remember { mutableStateOf(isIgnoringOptimizations(context)) }

    if (!requireDisabled || ignoring) {
        content()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Battery optimization", style = MaterialTheme.typography.titleLarge)

        Text(
            "To keep BLE scanning stable, disable battery optimization for this app."
        )

        StatusRow("Optimization disabled", ignoring)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { requestIgnore(context) },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Disable optimization")
            }

            OutlinedButton(
                onClick = { ignoring = isIgnoringOptimizations(context) },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Refresh")
            }
        }

        Text(
            "If the system does not allow disabling optimization, you can still proceed, " +
                    "but recording may drop scan events.",
            style = MaterialTheme.typography.bodySmall
        )
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

private fun isIgnoringOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestIgnore(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
