package com.bandik.mobileapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.bandik.mobileapp.ble.BleLiveScanner

class LiveBleViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = BleLiveScanner(app)

    val devices = scanner.devices

    fun start() = scanner.start()
    fun stop() = scanner.stop()
}
