package com.bandik.mobileapp.recording

import android.content.Intent

data class RecordingConfig(
    val experimentId: String,
    val pointId: String,

    val pointX: Double,
    val pointY: Double,
    val pointZ: Double,

    val roomWidthM: Double,
    val roomLengthM: Double,
    val roomHeightM: Double,

    val beaconLayoutJson: String,
    val beaconNames: List<String>,

    val durationSec: Int,
    val heightM: Double,
    val phonePose: String,
    val displayRotationDeg: Int,
    val notes: String,
    val appVersion: String
) {

    fun toIntent(i: Intent) = i.apply {
        putExtra("experimentId", experimentId)
        putExtra("pointId", pointId)

        putExtra("pointX", pointX)
        putExtra("pointY", pointY)
        putExtra("pointZ", pointZ)

        putExtra("roomWidthM", roomWidthM)
        putExtra("roomLengthM", roomLengthM)
        putExtra("roomHeightM", roomHeightM)

        putExtra("beaconLayoutJson", beaconLayoutJson)
        putStringArrayListExtra("beaconNames", ArrayList(beaconNames))

        putExtra("durationSec", durationSec)
        putExtra("heightM", heightM)
        putExtra("phonePose", phonePose)
        putExtra("displayRotationDeg", displayRotationDeg)
        putExtra("notes", notes)
        putExtra("appVersion", appVersion)
    }

    companion object {
        fun fromIntent(i: Intent?): RecordingConfig? {
            if (i == null) return null

            val exp = i.getStringExtra("experimentId") ?: return null
            val point = i.getStringExtra("pointId") ?: return null

            return RecordingConfig(
                experimentId = exp,
                pointId = point,

                pointX = i.getDoubleExtra("pointX", 0.0),
                pointY = i.getDoubleExtra("pointY", 0.0),
                pointZ = i.getDoubleExtra("pointZ", 0.0),

                roomWidthM = i.getDoubleExtra("roomWidthM", 0.0),
                roomLengthM = i.getDoubleExtra("roomLengthM", 0.0),
                roomHeightM = i.getDoubleExtra("roomHeightM", 0.0),

                beaconLayoutJson = i.getStringExtra("beaconLayoutJson") ?: "{}",
                beaconNames = i.getStringArrayListExtra("beaconNames") ?: emptyList(),

                durationSec = i.getIntExtra("durationSec", 10),
                heightM = i.getDoubleExtra("heightM", 1.0),
                phonePose = i.getStringExtra("phonePose") ?: "hand",
                displayRotationDeg = i.getIntExtra("displayRotationDeg", 0),
                notes = i.getStringExtra("notes") ?: "",
                appVersion = i.getStringExtra("appVersion") ?: "dev"
            )
        }
    }
}
