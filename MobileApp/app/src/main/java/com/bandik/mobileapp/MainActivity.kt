package com.bandik.mobileapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bandik.mobileapp.ble.BlePreflightGate
import com.bandik.mobileapp.ui.SetupScreen
import com.bandik.mobileapp.permissions.PermissionsGate
import com.bandik.mobileapp.power.BatteryOptimizationGate
import com.bandik.mobileapp.recording.RecordingService
import com.bandik.mobileapp.ui.RecordingScreen
import com.bandik.mobileapp.ui.SessionsScreen


private sealed class Screen {
    data object Setup : Screen()
    data object Recording : Screen()
    data object Sessions : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var screen by remember { mutableStateOf<Screen>(Screen.Setup) }

            PermissionsGate {
                BlePreflightGate {
                    BatteryOptimizationGate(requireDisabled = true) {
                        when (screen) {
                            Screen.Setup -> SetupScreen(onStart = { setup ->
                                // start service
                                val cfg = com.bandik.mobileapp.recording.RecordingConfig(
                                    experimentId = setup.experimentId,
                                    pointId = setup.pointId,
                                    durationSec = setup.durationSecText.toInt(),
                                    heightM = setup.heightMText.toDoubleOrNull() ?: 1.0,
                                    phonePose = setup.phonePose,
                                    displayRotationDeg = 0,
                                    notes = setup.notes,
                                    appVersion = "1.0.0"
                                )

                                val intent = Intent(
                                    this@MainActivity,
                                    RecordingService::class.java
                                ).apply {
                                    action = com.bandik.mobileapp.recording.RecordingService.ACTION_START
                                }
                                cfg.toIntent(intent)
                                startForegroundService(intent)

                                screen = Screen.Recording
                            },
                                onOpenSessions = { screen = Screen.Sessions }
                            )

                            Screen.Recording -> RecordingScreen(onDone = {
                                screen = Screen.Setup
                            })

                            Screen.Sessions -> SessionsScreen(onBack = { screen = Screen.Setup })
                        }
                    }
                }
            }
        }
    }
}
