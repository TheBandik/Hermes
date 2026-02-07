package com.bandik.mobileapp.recording

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.bandik.mobileapp.ble.BleScanner
import com.bandik.mobileapp.storage.JsonlWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class RecordingService : Service() {

    companion object {
        const val ACTION_START = "com.bandik.mobileapp.recording.START"
        const val ACTION_STOP = "com.bandik.mobileapp.recording.STOP"
        private const val CHANNEL_ID = "recording"
        private const val NOTIF_ID = 1001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null

    private var sessionFile: File? = null
    private var writer: JsonlWriter.SessionWriter? = null

    private val eventsWindow = ArrayDeque<Pair<Long, String>>() // (tElapsedMs, mac)
    private var startElapsedMs: Long = 0L
    private var startUtc: String = ""
    private var stoppedByUser: Boolean = false
    private var totalSamples: Long = 0
    private var droppedSamples: Long = 0

    private var ble: BleScanner? = null
    private var lastStartIntent: Intent? = null

    private val finishing = AtomicBoolean(false)

    private val json = Json { encodeDefaults = true }

    // One-way pipe: BLE callback -> channel -> single writer coroutine
    private var writeChannel: Channel<String>? = null
    private var writerJob: Job? = null

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START, null -> startRecording(intent)
            ACTION_STOP -> {
                stoppedByUser = true
                finishAndStop()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent?) {
        val cfg = RecordingConfig.fromIntent(intent) ?: run {
            RecordingBus.error("Missing recording config")
            stopSelf()
            return
        }

        lastStartIntent = intent

        // Reset state
        finishing.set(false)
        stoppedByUser = false
        totalSamples = 0
        droppedSamples = 0
        synchronized(eventsWindow) { eventsWindow.clear() }

        RecordingBus.reset()
        RecordingBus.update(LiveStats(isRecording = true, secondsLeft = cfg.durationSec))

        startForeground(NOTIF_ID, buildNotification("Recording…"))
        acquireWakeLock(cfg.durationSec)

        startElapsedMs = SystemClock.elapsedRealtime()
        startUtc = Instant.now().toString()

        val fileWriter = JsonlWriter(this)
        val file = fileWriter.newSessionFile(cfg.experimentId, cfg.pointId)
        sessionFile = file

        // Start streaming writer
        val ch = Channel<String>(capacity = 2048)
        writeChannel = ch
        writer = fileWriter.openSession(file)

        writerJob = scope.launch(Dispatchers.IO) {
            val w = writer ?: return@launch
            try {
                for (line in ch) {
                    w.appendLine(line)
                }
            } finally {
                w.close()
            }
        }

        // Write META line first
        val meta = MetaLine(
            schema = "ble-rssi-raw-jsonl/v1",
            experimentId = cfg.experimentId,
            pointId = cfg.pointId,
            startTimeUtc = startUtc,
            startTimeElapsedMs = startElapsedMs,
            durationSecPlanned = cfg.durationSec,
            receiverHeightM = cfg.heightM,
            displayRotationDeg = cfg.displayRotationDeg,
            phonePose = cfg.phonePose,
            notes = cfg.notes,
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            osName = "Android",
            osVersion = Build.VERSION.RELEASE ?: "unknown",
            sdk = Build.VERSION.SDK_INT,
            appVersion = cfg.appVersion
        )
        ch.trySend(json.encodeToString(meta))

        // Start BLE scanning
        ble = BleScanner(this) { rssi, mac, tx ->
            val nowElapsed = SystemClock.elapsedRealtime()
            val tElapsed = nowElapsed - startElapsedMs
            val tUtc = Instant.now().toString()

            val sampleLine = SampleLine(
                tElapsedMs = tElapsed,
                tUtc = tUtc,
                beaconType = "MAC",
                beaconValue = mac,
                rssi = rssi,
                txPower = tx
            )

            val ok = writeChannel?.trySend(json.encodeToString(sampleLine))?.isSuccess == true
            if (ok) {
                totalSamples++
            } else {
                // If channel is full, we drop (otherwise callback may block and scan will degrade)
                droppedSamples++
            }

            synchronized(eventsWindow) {
                eventsWindow.addLast(tElapsed to mac)
                trimWindowLocked(tElapsed)
            }
        }

        try {
            ble?.start()
        } catch (e: SecurityException) {
            RecordingBus.error("BLE scan permission missing: ${e.message}")
            finishAndStop()
            return
        } catch (e: Exception) {
            RecordingBus.error("BLE scan start failed: ${e.message}")
            finishAndStop()
            return
        }

        scope.launch {
            runTimer(cfg)
            finishAndStop()
        }
    }

    private suspend fun runTimer(cfg: RecordingConfig) {
        for (secLeft in cfg.durationSec downTo 1) {
            val now = SystemClock.elapsedRealtime() - startElapsedMs
            val (events2s, unique2s) = synchronized(eventsWindow) {
                trimWindowLocked(now)
                val list = eventsWindow.toList()
                list.size to list.map { it.second }.toSet().size
            }

            val eps = events2s / 2.0
            val receiving = events2s >= 2

            RecordingBus.update(
                LiveStats(
                    isRecording = true,
                    secondsLeft = secLeft,
                    eventsLast2s = events2s,
                    eventsPerSec = eps,
                    uniqueBeaconsLast2s = unique2s,
                    isReceiving = receiving,
                    outputFileName = sessionFile?.name,
                    lastError = if (droppedSamples > 0) "Dropped samples: $droppedSamples" else null
                )
            )

            delay(1000)
        }

        RecordingBus.update(
            RecordingBus.stats.value.copy(isRecording = true, secondsLeft = 0)
        )
    }

    private fun trimWindowLocked(nowElapsedMs: Long) {
        val cutoff = nowElapsedMs - 2000
        while (eventsWindow.isNotEmpty() && eventsWindow.first().first < cutoff) {
            eventsWindow.removeFirst()
        }
    }

    private fun finishAndStop() {
        if (!finishing.compareAndSet(false, true)) return

        // Do the heavy shutdown on background thread
        scope.launch(Dispatchers.IO) {
            try {
                try {
                    ble?.stop()
                } catch (_: Exception) {
                }
                ble = null

                val cfg = RecordingConfig.fromIntent(lastStartIntent)

                // END line
                if (cfg != null) {
                    val end = EndLine(
                        endTimeUtc = Instant.now().toString(),
                        stoppedByUser = stoppedByUser,
                        durationSecPlanned = cfg.durationSec,
                        durationMsElapsed = SystemClock.elapsedRealtime() - startElapsedMs,
                        totalSamples = totalSamples,
                        droppedSamples = droppedSamples
                    )
                    writeChannel?.trySend(json.encodeToString(end))
                }

                // Close writer pipeline
                writeChannel?.close()
                writerJob?.join()

            } catch (t: Throwable) {
                // If something really bad happens, at least report it
                RecordingBus.error("Finish failed: ${t.javaClass.simpleName}: ${t.message}")
            } finally {
                writeChannel = null
                writerJob = null
                writer = null

                releaseWakeLock()

                RecordingBus.update(
                    RecordingBus.stats.value.copy(
                        isRecording = false,
                        secondsLeft = 0
                    )
                )

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun acquireWakeLock(durationSec: Int) {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "blelogger:recording").apply {
            setReferenceCounted(false)
            acquire(durationSec * 1000L + 10_000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE Logger")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL_ID, "Recording", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(ch)
    }
}

@Serializable
private data class MetaLine(
    val type: String = "meta",
    val schema: String,
    val experimentId: String,
    val pointId: String,

    val startTimeUtc: String,
    val startTimeElapsedMs: Long,
    val durationSecPlanned: Int,

    val receiverHeightM: Double,
    val displayRotationDeg: Int,
    val phonePose: String,
    val notes: String,

    val deviceManufacturer: String,
    val deviceModel: String,
    val osName: String,
    val osVersion: String,
    val sdk: Int,
    val appVersion: String
)

@Serializable
private data class SampleLine(
    val type: String = "sample",
    val tElapsedMs: Long,
    val tUtc: String,
    val beaconType: String,
    val beaconValue: String,
    val rssi: Int,
    val txPower: Int? = null
)

@Serializable
private data class EndLine(
    val type: String = "end",
    val endTimeUtc: String,
    val stoppedByUser: Boolean,
    val durationSecPlanned: Int,
    val durationMsElapsed: Long,
    val totalSamples: Long,
    val droppedSamples: Long
)
