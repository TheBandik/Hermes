package com.bandik.mobileapp.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Build

class BleScanner(
    context: Context,
    private val onSample: (rssi: Int, mac: String, txPower: Int?) -> Unit
) {
    private val bluetoothAdapter: BluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val scanner: BluetoothLeScanner
        get() = bluetoothAdapter.bluetoothLeScanner

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val mac = result.device?.address ?: return
            val rssi = result.rssi
            val tx = if (Build.VERSION.SDK_INT >= 26) result.txPower else null
            onSample(rssi, mac, tx)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            for (r in results) onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, r)
        }

        override fun onScanFailed(errorCode: Int) {
            // handled by caller if needed
        }
    }

    fun start() {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        scanner.startScan(emptyList(), settings, callback)
    }

    fun stop() {
        scanner.stopScan(callback)
    }
}
