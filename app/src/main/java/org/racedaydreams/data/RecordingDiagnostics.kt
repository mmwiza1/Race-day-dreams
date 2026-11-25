package org.racedaydreams.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.time.Instant
import org.json.JSONObject
import org.racedaydreams.domain.recording.RunTroubleshooting
import org.racedaydreams.storage.recordingTroubleshootingJson

/** Device-local troubleshooting evidence. It is deliberately outside the user backup formats. */
internal class RecordingDiagnostics(context: Context) {
    private val directory = File(context.noBackupFilesDir, "recording-diagnostics")

    fun write(id: String, start: Instant, state: String, snapshot: RunTroubleshooting.Snapshot) =
        synchronized(lock) {
            val file = AtomicFile(path(id))
            val report = recordingTroubleshootingJson(id, start.toString(), state, snapshot)
            val stream = file.startWrite()
            try {
                stream.write(report.toString().toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (failure: IOException) {
                file.failWrite(stream)
                throw failure
            }
            files().drop(RETAINED_REPORTS).forEach { AtomicFile(it).delete() }
        }

    fun recent(): List<JSONObject> =
        synchronized(lock) {
            files().take(RETAINED_REPORTS).mapNotNull { file ->
                runCatching { JSONObject(AtomicFile(file).readFully().toString(Charsets.UTF_8)) }
                    .getOrNull()
            }
        }

    private fun path(id: String): File {
        require(id.matches(SESSION_ID))
        return File(directory, "$id.json")
    }

    private fun files() =
        directory
            .listFiles { path ->
                path.extension == "json" && path.nameWithoutExtension.matches(SESSION_ID)
            }
            .orEmpty()
            .sortedByDescending { it.lastModified() }

    private companion object {
        val lock = Any()
        val SESSION_ID = Regex("[a-f0-9-]{36}")
        const val RETAINED_REPORTS = 12
    }
}
