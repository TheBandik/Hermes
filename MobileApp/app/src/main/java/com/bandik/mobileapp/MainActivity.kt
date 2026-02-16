package com.bandik.mobileapp

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bandik.mobileapp.ble.BlePreflightGate
import com.bandik.mobileapp.ui.SetupScreen
import com.bandik.mobileapp.permissions.PermissionsGate
import com.bandik.mobileapp.power.BatteryOptimizationGate
import com.bandik.mobileapp.recording.RecordingService
import com.bandik.mobileapp.ui.LiveBleScreen
import com.bandik.mobileapp.ui.RecordingScreen
import com.bandik.mobileapp.ui.SessionsScreen


private sealed class Screen {
    data object Setup : Screen()
    data object Recording : Screen()
    data object Sessions : Screen()
}

class MainActivity : ComponentActivity() {
    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launcher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

//        setContent {
//            LiveBleScreen()
//        }
        setContent {
            var screen by remember { mutableStateOf<Screen>(Screen.Setup) }

            PermissionsGate {
                BlePreflightGate {
                    BatteryOptimizationGate(requireDisabled = true) {
                        when (screen) {
                            Screen.Setup -> SetupScreen(
                                onStart = { setup ->

                                    val beaconNames = listOf(
                                        "Sd499d1",
                                        "Sdb00b2",
                                        "Sd3708a",
                                        "Sea2540"
                                    )

                                    val beaconLayoutJson = """
        [
          {"id":"Sd499d1","x":0.0,"y":0.0,"z":2.5},
          {"id":"Sdb00b2","x":6.0,"y":0.0,"z":2.5},
          {"id":"Sd3708a","x":0.0,"y":6.0,"z":2.5},
          {"id":"Sea2540","x":6.0,"y":6.0,"z":2.5}
        ]
        """.trimIndent()

                                    val cfg = com.bandik.mobileapp.recording.RecordingConfig(
                                        experimentId = setup.experimentId,
                                        pointId = setup.pointId,

                                        pointX = setup.pointXText.toDouble(),
                                        pointY = setup.pointYText.toDouble(),
                                        pointZ = setup.pointZText.toDouble(),

                                        roomWidthM = setup.roomWidthText.toDouble(),
                                        roomLengthM = setup.roomLengthText.toDouble(),
                                        roomHeightM = setup.roomHeightText.toDouble(),

                                        beaconLayoutJson = beaconLayoutJson,
                                        beaconNames = beaconNames,

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
                                        action = RecordingService.ACTION_START
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
