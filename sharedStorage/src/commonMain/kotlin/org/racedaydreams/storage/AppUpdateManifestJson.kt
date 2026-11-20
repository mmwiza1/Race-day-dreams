package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.update.AvailableUpdate

/**
 * The release manifest `Race-Day-Dreams-latest-update.json`, written by CI only after the APK was
 * built from one verified commit, signed with the project's release certificate and checked.
 *
 * Decoding is strict: unknown schema versions, missing or extra fields and wrong types are
 * rejected, so a malformed or unexpected manifest never offers an update.
 */
object AppUpdateManifestJson {
    const val SCHEMA = 1

    /** A manifest is a few kilobytes; anything larger is not ours. */
    const val MAX_CHARS = 64 * 1024

    private val FIELDS =
        setOf(
            "schema",
            "packageName",
            "versionName",
            "versionCode",
            "minSdk",
            "apkName",
            "apkUrl",
            "apkBytes",
            "apkSha256",
            "certificateSha256",
            "commit",
            "notes",
        )

    fun decode(text: String, expectedPackage: String): AvailableUpdate {
        require(text.length <= MAX_CHARS) { "The update information is too large." }
        val root =
            try {
                Json.parseToJsonElement(text)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("The update information could not be read.")
            }
        require(root is JsonObject) { "The update information could not be read." }
        require(root.keys == FIELDS) { "The update information has an unexpected format." }
        require(whole(root, "schema") == SCHEMA.toLong()) {
            "This update needs a newer version of Race Day Dreams to read it."
        }
        require(text(root, "packageName") == expectedPackage) {
            "The update is for a different app."
        }
        val minSdk = whole(root, "minSdk")
        require(minSdk in 1..1_000) { "Unexpected minimum Android version." }
        return AvailableUpdate(
            versionName = text(root, "versionName"),
            versionCode = whole(root, "versionCode"),
            minSdk = minSdk.toInt(),
            apkName = text(root, "apkName"),
            apkUrl = text(root, "apkUrl"),
            apkBytes = whole(root, "apkBytes"),
            apkSha256 = text(root, "apkSha256"),
            certificateSha256 = text(root, "certificateSha256"),
            commit = text(root, "commit"),
            notes = text(root, "notes"),
        )
    }

    fun encode(update: AvailableUpdate, packageName: String): String =
        buildJsonObject {
                put("schema", SCHEMA)
                put("packageName", packageName)
                put("versionName", update.versionName)
                put("versionCode", update.versionCode)
                put("minSdk", update.minSdk)
                put("apkName", update.apkName)
                put("apkUrl", update.apkUrl)
                put("apkBytes", update.apkBytes)
                put("apkSha256", update.apkSha256)
                put("certificateSha256", update.certificateSha256)
                put("commit", update.commit)
                put("notes", update.notes)
            }
            .toString()

    private fun text(root: JsonObject, name: String): String {
        val value = root.getValue(name)
        require(value is JsonPrimitive && value.isString) { "Unexpected $name." }
        return value.content
    }

    private fun whole(root: JsonObject, name: String): Long {
        val value = root.getValue(name)
        require(value is JsonPrimitive && !value.isString) { "Unexpected $name." }
        return value.longOrNull ?: throw IllegalArgumentException("Unexpected $name.")
    }
}
