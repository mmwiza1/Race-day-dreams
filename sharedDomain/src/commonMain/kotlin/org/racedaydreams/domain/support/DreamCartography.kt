package org.racedaydreams.domain.support

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable

/**
 * Reproducible geographic context, not a street map or navigation. Wikidata P625 landmarks: CC0.
 * Natural Earth river centrelines: public domain. NYC projected landmark DATA: ODbL-1.0, (c)
 * OpenStreetMap contributors, prepared by MARATHON TRIP. Coordinate data retain their own licences.
 * Original wrapper code/illustrations: MIT. Exact sources/positions/transform:
 * docs/dream-route-data/city-context.json, generate-dream-context.py.
 */
object DreamCartography {
    data class River(private val label: String?, private var vertices: List<DreamRoutes.Vertex>?) {
        init {
            vertices = Immutable.copyList(vertices ?: throw NullPointerException())
        }

        fun label(): String? = label

        fun vertices(): List<DreamRoutes.Vertex> = vertices!!

        override fun toString(): String = "River[label=$label, vertices=$vertices]"
    }

    @JvmStatic
    fun landmarks(routeId: String): List<DreamRoutes.Landmark> =
        when (routeId) {
            "london-historical-geo-v1" ->
                Immutable.list(
                    DreamRoutes.Landmark("Tower Bridge", 3.37495706, 4.10068193, "bridge"),
                    DreamRoutes.Landmark("Buckingham Palace", 0.42800707, 4.42389407, "palace"),
                    DreamRoutes.Landmark("London Eye", 1.41195956, 4.25834639, "wheel"),
                )
            "berlin-2014-geo-v1" ->
                Immutable.list(
                    DreamRoutes.Landmark("Brandenburg Gate", 6.37642716, 3.24484766, "gate"),
                    DreamRoutes.Landmark("Reichstag", 6.27829438, 3.01081475, "dome"),
                    DreamRoutes.Landmark("Victory Column", 4.69462774, 3.42117840, "column"),
                )
            "paris-2024-olympic-geo-v1" ->
                Immutable.list(
                    DreamRoutes.Landmark("Eiffel Tower", 7.21908250, 3.62064641, "tower"),
                    DreamRoutes.Landmark("Versailles", 0.25385066, 6.87481038, "palace"),
                    DreamRoutes.Landmark("Louvre", 8.87258709, 3.44965169, "pyramid"),
                )
            "tokyo-2026-geo-v1" ->
                Immutable.list(
                    DreamRoutes.Landmark("Tokyo Tower", 4.97410241, 7.04803257, "tower"),
                    DreamRoutes.Landmark("Tokyo Station", 6.76750581, 4.75393490, "station"),
                    DreamRoutes.Landmark("Sensō-ji", 9.25730871, 1.27264169, "temple"),
                )
            "new-york-2026-geo-v1" ->
                Immutable.list(
                    DreamRoutes.Landmark(
                        "Verrazzano Bridge",
                        3.28037253,
                        9.34829619,
                        "suspension-bridge",
                    ),
                    DreamRoutes.Landmark(
                        "Queensboro Bridge",
                        6.12943105,
                        2.95138871,
                        "cantilever-bridge",
                    ),
                    DreamRoutes.Landmark("Central Park finish", 5.33969456, 2.28358702, "park"),
                )
            "paris-2026-geo-v1" ->
                Immutable.list(
                    DreamRoutes.Landmark("Eiffel Tower", 2.70592686, 4.21051545, "tower"),
                    DreamRoutes.Landmark("Château de Vincennes", 8.59377175, 5.19279606, "palace"),
                    DreamRoutes.Landmark("Louvre", 4.42846267, 4.03232306, "pyramid"),
                )
            else -> Immutable.list()
        }

    @JvmStatic
    fun rivers(routeId: String): List<River> =
        when (routeId) {
            "london-historical-geo-v1" ->
                Immutable.list(
                    River(
                        "Thames",
                        Immutable.list(
                            DreamRoutes.Vertex(-2.05919419, 6.80827733),
                            DreamRoutes.Vertex(-1.21811356, 6.61770830),
                            DreamRoutes.Vertex(-0.22249104, 6.10098668),
                            DreamRoutes.Vertex(0.71566963, 5.40518978),
                            DreamRoutes.Vertex(2.18573882, 3.85204507),
                            DreamRoutes.Vertex(2.95491485, 3.66729386),
                            DreamRoutes.Vertex(3.73133435, 3.92136225),
                            DreamRoutes.Vertex(4.54542867, 4.42942809),
                            DreamRoutes.Vertex(5.03608480, 4.67193179),
                            DreamRoutes.Vertex(6.40911812, 4.91443550),
                            DreamRoutes.Vertex(7.13876429, 4.45255747),
                            DreamRoutes.Vertex(7.61321097, 4.32548780),
                            DreamRoutes.Vertex(8.83170241, 5.12231609),
                            DreamRoutes.Vertex(9.87767581, 5.02703157),
                        ),
                    )
                )
            "paris-2024-olympic-geo-v1" ->
                Immutable.list(
                    River(
                        "Seine",
                        Immutable.list(
                            DreamRoutes.Vertex(12.47923506, 12.33009943),
                            DreamRoutes.Vertex(13.07471423, 11.32166522),
                            DreamRoutes.Vertex(13.16255870, 10.62714428),
                            DreamRoutes.Vertex(12.54756743, 9.78684318),
                            DreamRoutes.Vertex(12.31977956, 9.36663189),
                            DreamRoutes.Vertex(12.22541772, 8.93415077),
                            DreamRoutes.Vertex(12.25144719, 7.66124706),
                            DreamRoutes.Vertex(12.16688139, 7.35972511),
                            DreamRoutes.Vertex(11.92933747, 6.68743564),
                            DreamRoutes.Vertex(11.07356358, 5.10566024),
                            DreamRoutes.Vertex(9.79152210, 3.79570408),
                            DreamRoutes.Vertex(8.34026905, 3.10367355),
                            DreamRoutes.Vertex(6.99317387, 3.37306312),
                            DreamRoutes.Vertex(5.63304397, 4.82387935),
                            DreamRoutes.Vertex(4.85535850, 5.10808991),
                            DreamRoutes.Vertex(4.51369668, 3.95387555),
                            DreamRoutes.Vertex(4.76751403, 2.96524314),
                            DreamRoutes.Vertex(5.37922662, 1.92224689),
                            DreamRoutes.Vertex(6.10485315, 1.01762030),
                            DreamRoutes.Vertex(6.71984442, 0.42939738),
                            DreamRoutes.Vertex(8.20360432, -0.33679881),
                            DreamRoutes.Vertex(8.53227139, -0.77924157),
                            DreamRoutes.Vertex(8.63315059, -1.84695971),
                            DreamRoutes.Vertex(8.19384827, -2.44259312),
                            DreamRoutes.Vertex(7.22743912, -2.35852656),
                            DreamRoutes.Vertex(5.62652660, -1.63934447),
                            DreamRoutes.Vertex(4.61133719, -0.63091026),
                            DreamRoutes.Vertex(3.99634592, -0.17364652),
                            DreamRoutes.Vertex(3.29351018, 0.01416691),
                            DreamRoutes.Vertex(2.79239285, 0.30335829),
                            DreamRoutes.Vertex(1.95613108, 1.66518789),
                            DreamRoutes.Vertex(1.50711268, 2.12737171),
                            DreamRoutes.Vertex(0.43982434, 1.92716697),
                            DreamRoutes.Vertex(0.31291567, 0.43680787),
                            DreamRoutes.Vertex(0.89212141, -1.25375597),
                            DreamRoutes.Vertex(2.10259184, -2.20035510),
                            DreamRoutes.Vertex(2.33361840, -2.55873130),
                            DreamRoutes.Vertex(2.52234207, -3.03330644),
                            DreamRoutes.Vertex(2.60043049, -3.50782084),
                            DreamRoutes.Vertex(2.53857551, -4.07381229),
                            DreamRoutes.Vertex(2.36612526, -4.29381883),
                            DreamRoutes.Vertex(2.10906922, -4.38280547),
                            DreamRoutes.Vertex(1.25329533, -4.81528659),
                        ),
                    ),
                    River(
                        "Seine",
                        Immutable.list(
                            DreamRoutes.Vertex(-1.64589211, -4.69422832),
                            DreamRoutes.Vertex(-2.11774127, -3.95275402),
                            DreamRoutes.Vertex(-2.88563070, -2.05700461),
                            DreamRoutes.Vertex(-3.57871039, -1.51324464),
                            DreamRoutes.Vertex(-4.22300980, -1.74558176),
                            DreamRoutes.Vertex(-4.54511953, -2.54640073),
                            DreamRoutes.Vertex(-4.24576060, -3.71543608),
                        ),
                    ),
                )
            "paris-2026-geo-v1" ->
                Immutable.list(
                    River(
                        "Seine",
                        Immutable.list(
                            DreamRoutes.Vertex(8.18568254, 13.28657480),
                            DreamRoutes.Vertex(8.80602202, 12.23569264),
                            DreamRoutes.Vertex(8.89753386, 11.51193728),
                            DreamRoutes.Vertex(8.25686767, 10.63626545),
                            DreamRoutes.Vertex(8.01957001, 10.19836624),
                            DreamRoutes.Vertex(7.92126872, 9.74768073),
                            DreamRoutes.Vertex(7.94838488, 8.42119679),
                            DreamRoutes.Vertex(7.86028859, 8.10698290),
                            DreamRoutes.Vertex(7.61282758, 7.40639480),
                            DreamRoutes.Vertex(6.72132648, 5.75803784),
                            DreamRoutes.Vertex(5.38576177, 4.39294180),
                            DreamRoutes.Vertex(3.87392118, 3.67178168),
                            DreamRoutes.Vertex(2.47058690, 3.95251065),
                            DreamRoutes.Vertex(1.05367371, 5.46439598),
                            DreamRoutes.Vertex(0.24352109, 5.76056979),
                            DreamRoutes.Vertex(-0.11240457, 4.55777117),
                            DreamRoutes.Vertex(0.15200925, 3.52752434),
                            DreamRoutes.Vertex(0.78925989, 2.44062532),
                            DreamRoutes.Vertex(1.54518018, 1.49792035),
                            DreamRoutes.Vertex(2.18584637, 0.88493742),
                            DreamRoutes.Vertex(3.73155092, 0.08648979),
                            DreamRoutes.Vertex(4.07393933, -0.37457668),
                            DreamRoutes.Vertex(4.17903008, -1.48723820),
                            DreamRoutes.Vertex(3.72138757, -2.10794356),
                            DreamRoutes.Vertex(2.71463236, -2.02033839),
                            DreamRoutes.Vertex(1.04688426, -1.27088383),
                            DreamRoutes.Vertex(-0.01068772, -0.22000168),
                            DreamRoutes.Vertex(-0.65135391, 0.25650964),
                            DreamRoutes.Vertex(-1.38353194, 0.45222868),
                            DreamRoutes.Vertex(-1.90557013, 0.75359297),
                            DreamRoutes.Vertex(-2.77674452, 2.17274595),
                            DreamRoutes.Vertex(-3.24450873, 2.65438445),
                            DreamRoutes.Vertex(-4.35635469, 2.44575250),
                            DreamRoutes.Vertex(-4.48856160, 0.89265984),
                            DreamRoutes.Vertex(-3.88517492, -0.86906478),
                            DreamRoutes.Vertex(-2.62416925, -1.85550903),
                            DreamRoutes.Vertex(-2.38349769, -2.22897034),
                            DreamRoutes.Vertex(-2.18689510, -2.72352174),
                            DreamRoutes.Vertex(-2.10554662, -3.21800983),
                            DreamRoutes.Vertex(-2.16998395, -3.80782551),
                            DreamRoutes.Vertex(-2.34963373, -4.03709278),
                            DreamRoutes.Vertex(-2.61742145, -4.12982512),
                            DreamRoutes.Vertex(-3.50892255, -4.58051063),
                        ),
                    ),
                    River(
                        "Seine",
                        Immutable.list(
                            DreamRoutes.Vertex(-6.52914652, -4.45435667),
                            DreamRoutes.Vertex(-7.02069464, -3.68167154),
                            DreamRoutes.Vertex(-7.82064225, -1.70612450),
                            DreamRoutes.Vertex(-8.54265693, -1.13947609),
                            DreamRoutes.Vertex(-9.21385484, -1.38159295),
                            DreamRoutes.Vertex(-9.54941214, -2.21612074),
                            DreamRoutes.Vertex(-9.23755544, -3.43436420),
                        ),
                    ),
                )
            else -> Immutable.list()
        }
}
