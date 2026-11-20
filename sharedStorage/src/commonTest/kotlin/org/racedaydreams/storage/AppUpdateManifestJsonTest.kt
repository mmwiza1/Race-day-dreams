package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.update.AppUpdates
import org.racedaydreams.domain.update.AvailableUpdate

class AppUpdateManifestJsonTest {
    private val pkg = AppUpdates.PACKAGE_NAME
    private val update =
        AvailableUpdate(
            versionName = "0.15.0-test1",
            versionCode = 23,
            minSdk = 28,
            apkName = "Race-Day-Dreams-0.15.0-test1.apk",
            apkUrl = AppUpdates.RELEASE_DOWNLOAD_PREFIX + "Race-Day-Dreams-0.15.0-test1.apk",
            apkBytes = 59_549_417,
            apkSha256 = "d8cf2fb37343bcfc5bc9f85cdcf482c0ead4d5c4b2f11e7128e4db817feb7e18",
            certificateSha256 = "1".repeat(64),
            commit = "44987a3bd9e0a03686a5caad64524ed7832de908",
            notes = "• Check for updates in Settings\n• \"Quoted\" text and ünïcode",
        )

    private fun edited(change: MutableMap<String, JsonElement>.() -> Unit): String {
        val fields =
            Json.parseToJsonElement(AppUpdateManifestJson.encode(update, pkg))
                .jsonObject
                .toMutableMap()
        fields.change()
        return JsonObject(fields).toString()
    }

    private fun rejected(text: String) =
        assertFailsWith<IllegalArgumentException> { AppUpdateManifestJson.decode(text, pkg) }

    @Test
    fun theCiManifestRoundTripsExactly() {
        assertEquals(
            update,
            AppUpdateManifestJson.decode(AppUpdateManifestJson.encode(update, pkg), pkg),
        )
    }

    @Test
    fun theFormatWrittenByTheReleaseScriptIsAccepted() {
        // Mirrors scripts/development-assets.py update_manifest(): key order and spacing differ.
        val text =
            """
            {"apkBytes": 59549417, "apkName": "Race-Day-Dreams-0.15.0-test1.apk",
             "apkSha256": "d8cf2fb37343bcfc5bc9f85cdcf482c0ead4d5c4b2f11e7128e4db817feb7e18",
             "apkUrl": "https://github.com/mmwiza1/Race-day-dreams/releases/download/development/Race-Day-Dreams-0.15.0-test1.apk",
             "certificateSha256": "${"1".repeat(64)}", "commit": "44987a3bd9e0a03686a5caad64524ed7832de908",
             "minSdk": 28, "notes": "• Check for updates in Settings\n• \"Quoted\" text and ünïcode",
             "packageName": "org.racedaydreams", "schema": 1, "versionCode": 23, "versionName": "0.15.0-test1"}
            """
        assertEquals(update, AppUpdateManifestJson.decode(text, pkg))
    }

    @Test
    fun malformedOrUnexpectedManifestsNeverOfferAnUpdate() {
        rejected("")
        rejected("not json")
        rejected("[]")
        rejected("<html>404</html>")
        rejected(" ".repeat(AppUpdateManifestJson.MAX_CHARS + 1))
        rejected(edited { put("schema", JsonPrimitive(2)) })
        rejected(edited { remove("certificateSha256") })
        rejected(edited { put("extra", JsonPrimitive("field")) })
        rejected(edited { put("versionCode", JsonPrimitive("23")) })
        rejected(edited { put("versionCode", JsonPrimitive(23.5)) })
        rejected(edited { put("apkBytes", JsonNull) })
        rejected(edited { put("packageName", JsonPrimitive("com.example.other")) })
        rejected(edited { put("minSdk", JsonPrimitive(0)) })
        rejected(
            edited {
                put(
                    "apkUrl",
                    JsonPrimitive("https://evil.example/Race-Day-Dreams-0.15.0-test1.apk"),
                )
            }
        )
        rejected(edited { put("apkSha256", JsonPrimitive("0".repeat(63))) })
    }
}
