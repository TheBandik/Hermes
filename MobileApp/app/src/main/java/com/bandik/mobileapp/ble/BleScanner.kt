package com.bandik.mobileapp.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.util.Base64

class BleScanner(
    context: Context,
    private val allowedBeaconNames: List<String>,
    private val onSample: (
        rssi: Int,
        mac: String,
        beaconId: String,
        txPower: Int?,
        advBase64: String?,
        callbackType: Int
    ) -> Unit
) {

    private val bluetoothAdapter: BluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val scanner: BluetoothLeScanner
        get() = bluetoothAdapter.bluetoothLeScanner

    private val callback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {

            val scanRecord = result.scanRecord ?: return

            // Берём advertising name ТОЛЬКО из scanRecord
            val beaconId = scanRecord.deviceName ?: return

            if (!allowedBeaconNames.contains(beaconId)) return

            val mac = result.device?.address ?: return
            val rssi = result.rssi
            val txPower = if (Build.VERSION.SDK_INT >= 26) result.txPower else null

            val advBytes = scanRecord.bytes
            val advBase64 = advBytes?.let {
                Base64.encodeToString(it, Base64.NO_WRAP)
            }

            onSample(
                rssi,
                mac,
                beaconId,
                txPower,
                advBase64,
                callbackType
            )
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            for (r in results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, r)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // можно добавить логирование
        }
    }

    fun start() {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val filters = allowedBeaconNames.map {
            ScanFilter.Builder()
                .setDeviceName(it)
                .build()
        }

        scanner.startScan(filters, settings, callback)
    }

    fun stop() {
        scanner.stopScan(callback)
    }
}
