package org.racedaydreams.domain.settings

/** Portable appearance preferences. Missing or unrecognised values use the app's default. */
enum class ColourDiscipline(val key: String, val label: String) {
    RUNNING("appearance.accent.running", "Running"),
    STRENGTH("appearance.accent.strength", "Gym / Strength"),
    BOULDERING("appearance.accent.bouldering", "Bouldering"),
    PROGRESS("appearance.accent.progress", "Progress"),
    SETTINGS("appearance.accent.settings", "Settings"),
}

enum class AccentColour(val label: String) {
    BLUE("Blue"),
    TEAL("Teal"),
    GREEN("Green"),
    VIOLET("Violet"),
    PLUM("Plum"),
    AMBER("Amber"),
}

object DisciplineColours {
    fun read(settings: SettingsRepository): Map<ColourDiscipline, AccentColour> =
        ColourDiscipline.entries
            .mapNotNull { discipline ->
                val stored = settings.getSetting(discipline.key)
                AccentColour.entries.firstOrNull { it.name == stored }?.let { discipline to it }
            }
            .toMap()

    fun save(settings: SettingsRepository, discipline: ColourDiscipline, colour: AccentColour?) {
        if (colour == null) settings.removeSetting(discipline.key)
        else settings.putSetting(discipline.key, colour.name)
    }
}
