package com.bandik.mobileapp.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class LiveBleDevice(
    val name: String?,
    val address: String,
    val rssi: Int
)

class BleLiveScanner(context: Context) {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val scanner = bluetoothManager.adapter.bluetoothLeScanner

    private val _devices = MutableStateFlow<List<LiveBleDevice>>(emptyList())
    val devices: StateFlow<List<LiveBleDevice>> = _devices

    private val callback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {

            val device = LiveBleDevice(
                name = result.device.name
                    ?: result.scanRecord?.deviceName,
                address = result.device.address,
                rssi = result.rssi
            )

            _devices.update { current ->
                (current + device)
                    .distinctBy { it.address }
                    .sortedByDescending { it.rssi }
            }
        }
    }

    fun start() = scanner.startScan(callback)
    fun stop() = scanner.stopScan(callback)
}
