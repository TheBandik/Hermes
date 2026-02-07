package com.bandik.mobileapp.recording

import android.content.Intent

data class RecordingConfig(
    val experimentId: String,
    val pointId: String,
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
