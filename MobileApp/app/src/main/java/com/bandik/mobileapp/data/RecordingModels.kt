package com.bandik.mobileapp.data

import kotlinx.serialization.Serializable

@Serializable
data class RecordingLine(
    val schema: String = "ble-rssi-raw/v1",
    val experimentId: String,
    val pointId: String,
    val recording: RecordingMeta,
    val device: DeviceMeta,
    val metadata: UserMeta,
    val scanConfig: ScanConfig,
    val samples: List<Sample>
)

@Serializable
data class RecordingMeta(
    val startTimeUtc: String,
    val startTimeElapsedMs: Long,
    val durationSec: Int,
    val stoppedByUser: Boolean
)

@Serializable
data class DeviceMeta(
    val manufacturer: String,
    val model: String,
    val osName: String,
    val osVersion: String,
    val sdk: Int,
    val appVersion: String
)

@Serializable
data class UserMeta(
    val receiverHeightM: Double,
    val displayRotationDeg: Int,
    val phonePose: String,
    val notes: String = ""
)

@Serializable
data class ScanConfig(
    val scanMode: String = "LOW_LATENCY",
    val callbackType: String = "ALL_MATCHES",
    val filters: List<String> = emptyList()
)

@Serializable
data class Sample(
    val tElapsedMs: Long,
    val tUtc: String,
    val beaconType: String, // "MAC"
    val beaconValue: String,
    val rssi: Int,
    val txPower: Int? = null
)
