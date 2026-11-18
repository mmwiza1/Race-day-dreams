package org.racedaydreams.coach

data class DreamLandmarkPoster(val label: String, val asset: String)

data class DreamCityPresentation(
    val city: String,
    val course: String,
    val edition: String,
    val hero: String,
    val landmarks: List<DreamLandmarkPoster>,
    val geographic: Boolean,
)

/** Identity is derived from the retained geometry ID, never from today's preference. */
fun dreamCityPresentation(
    route: org.racedaydreams.domain.support.DreamRoutes.Route
): DreamCityPresentation {
    val city =
        when {
            route.id().startsWith("new-york") -> "new-york"
            else -> route.id().substringBefore('-')
        }
    fun poster(label: String, slug: String) = DreamLandmarkPoster(label, "$city-$slug")
    val all =
        when (city) {
            "london" ->
                listOf(
                    poster("Tower Bridge", "tower-bridge"),
                    poster("Buckingham Palace", "buckingham-palace"),
                    poster("London Eye", "london-eye"),
                )
            "berlin" ->
                listOf(
                    poster("Brandenburg Gate", "brandenburg-gate"),
                    poster("Reichstag", "reichstag"),
                    poster("Victory Column", "victory-column"),
                )
            "paris" ->
                listOf(
                    poster("Eiffel Tower", "eiffel-tower"),
                    poster("Château de Vincennes", "vincennes"),
                    poster("Louvre", "louvre"),
                )
            "tokyo" ->
                listOf(
                    poster("Tokyo Tower", "tokyo-tower"),
                    poster("Tokyo Station", "tokyo-station"),
                    poster("Sensō-ji", "senso-ji"),
                )
            "new-york" ->
                listOf(
                    poster("Verrazzano Bridge", "verrazzano"),
                    poster("Queensboro Bridge", "queensboro"),
                    poster("Central Park finish", "central-park"),
                )
            else -> error("Unknown retained Dream city")
        }
    val geographic = route.id().contains("-geo-")
    val course =
        when (route.id()) {
            "paris-2024-olympic-geo-v1" -> "Paris 2024 Olympic marathon course"
            "paris-2026-geo-v1" -> "Marathon de Paris"
            else ->
                if (geographic) "${route.name()} Marathon course"
                else "${route.name()} course-inspired marathon equivalent"
        }
    val edition =
        when (route.id()) {
            "london-historical-geo-v1" -> "Historical course · edition unspecified"
            "berlin-2014-geo-v1" -> "2014 course"
            "paris-2026-geo-v1",
            "tokyo-2026-geo-v1",
            "new-york-2026-geo-v1" -> "2026 course"
            "paris-2024-olympic-geo-v1" -> "Archived Olympic course · 2024"
            else -> "Archived course-inspired drawing"
        }
    // Do not relabel a historic assignment's landmarks as belonging to the annual course.
    val landmarks =
        if (geographic) {
            val retained =
                org.racedaydreams.domain.support.DreamRoutes.landmarks(route)
                    .map { it.label() }
                    .toSet()
            all.filter { it.label in retained }
        } else all
    val displayCity =
        when (city) {
            "london" -> "London"
            "berlin" -> "Berlin"
            "paris" -> "Paris"
            "tokyo" -> "Tokyo"
            else -> "New York"
        }
    return DreamCityPresentation(displayCity, course, edition, "$city-hero", landmarks, geographic)
}
