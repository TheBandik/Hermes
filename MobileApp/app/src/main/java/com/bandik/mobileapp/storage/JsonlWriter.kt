package com.bandik.mobileapp.storage

import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class JsonlWriter(private val context: Context) {

    fun newSessionFile(experimentId: String, pointId: String): File {
        val dir = File(context.filesDir, "sessions").apply { mkdirs() }
        val safeExp = experimentId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safePoint = pointId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val name = "session_${safeExp}_${safePoint}_${System.currentTimeMillis()}.jsonl"
        return File(dir, name)
    }

    fun openSession(file: File): SessionWriter {
        val fos = file.outputStream().buffered()
        val osw = OutputStreamWriter(fos, StandardCharsets.UTF_8)
        val bw = BufferedWriter(osw)
        return SessionWriter(bw)
    }

    class SessionWriter(private val bw: BufferedWriter) {
        fun appendLine(line: String) {
            bw.write(line)
            bw.newLine()
        }

        fun close() {
            bw.flush()
            bw.close()
        }
    }
}
