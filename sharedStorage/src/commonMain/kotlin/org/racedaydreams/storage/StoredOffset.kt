package org.racedaydreams.storage

import org.racedaydreams.domain.time.Offsets
import org.racedaydreams.domain.time.ZoneOffset

internal object StoredOffset {
    fun parse(text: String): ZoneOffset {
        if (text == "Z") return Offsets.ofTotalSeconds(0)
        val parts =
            Regex("([+-])([0-9]{2}):([0-9]{2})(?::([0-9]{2}))?").matchEntire(text)?.groupValues
                ?: throw IllegalArgumentException("Invalid stored source offset.")
        val hours = parts[2].toInt()
        val minutes = parts[3].toInt()
        val seconds = parts[4].ifEmpty { "0" }.toInt()
        require(hours <= 18 && minutes <= 59 && seconds <= 59)
        val total = (hours * 3600 + minutes * 60 + seconds) * if (parts[1] == "-") -1 else 1
        require(total in -64800..64800) { "Invalid stored source offset." }
        val result = Offsets.ofTotalSeconds(total)
        require(result.toString() == text)
        return result
    }
}
