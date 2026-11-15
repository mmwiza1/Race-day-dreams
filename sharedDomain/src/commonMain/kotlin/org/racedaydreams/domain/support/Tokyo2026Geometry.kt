package org.racedaydreams.domain.support

import kotlin.jvm.JvmStatic

/**
 * Geographic coordinates from kekomat/marathon-gpx-dataset, CC0-1.0. Full notice:
 * docs/dream-route-data/TOKYO_SOURCE_LICENCE.txt. Tokyo2026 approximate participant GPS trace, not
 * certified/official geometry. Position-only original vertices, simplified25m; north-up uniform
 * projection. Marathon-equivalent progress is separate from the42,746.8m observed trace.
 */
internal object Tokyo2026Geometry {
    @JvmStatic
    fun route(): DreamRoutes.Route {
        val points = ArrayList<DreamRoutes.Vertex>(91)
        points.addAll(part0())
        return DreamRoutes.Route("tokyo-2026-geo-v1", "Tokyo", points)
    }

    private fun part0(): List<DreamRoutes.Vertex> =
        listOf(
            DreamRoutes.Vertex(0.50000000, 3.78581005),
            DreamRoutes.Vertex(0.55874011, 4.07628284),
            DreamRoutes.Vertex(0.65921958, 4.07126410),
            DreamRoutes.Vertex(0.57902998, 3.56918202),
            DreamRoutes.Vertex(0.69182955, 3.54464209),
            DreamRoutes.Vertex(0.86250478, 3.59510643),
            DreamRoutes.Vertex(0.93207506, 3.47323748),
            DreamRoutes.Vertex(1.13222577, 3.43827068),
            DreamRoutes.Vertex(1.26017070, 3.44474313),
            DreamRoutes.Vertex(1.70274561, 3.58473147),
            DreamRoutes.Vertex(2.27938685, 3.67589956),
            DreamRoutes.Vertex(2.59183111, 3.68398146),
            DreamRoutes.Vertex(2.96983968, 3.56619673),
            DreamRoutes.Vertex(3.08705284, 3.56352295),
            DreamRoutes.Vertex(3.31700935, 3.58909259),
            DreamRoutes.Vertex(3.51370228, 3.67647931),
            DreamRoutes.Vertex(3.86285384, 3.66645913),
            DreamRoutes.Vertex(4.05280691, 3.60497951),
            DreamRoutes.Vertex(4.25776478, 3.43279332),
            DreamRoutes.Vertex(4.80748171, 2.60893187),
            DreamRoutes.Vertex(4.90112292, 2.51329018),
            DreamRoutes.Vertex(5.03882976, 2.45136061),
            DreamRoutes.Vertex(5.42874378, 2.46695333),
            DreamRoutes.Vertex(5.78123365, 2.53576204),
            DreamRoutes.Vertex(5.80639393, 2.69213918),
            DreamRoutes.Vertex(6.03445990, 3.17854202),
            DreamRoutes.Vertex(6.24019788, 3.17851606),
            DreamRoutes.Vertex(6.43873215, 3.33703915),
            DreamRoutes.Vertex(6.61228887, 3.32436249),
            DreamRoutes.Vertex(6.90549044, 3.19308772),
            DreamRoutes.Vertex(7.06209560, 3.18419243),
            DreamRoutes.Vertex(7.11723737, 2.99376570),
            DreamRoutes.Vertex(7.19044815, 2.18599019),
            DreamRoutes.Vertex(7.27062370, 1.98125139),
            DreamRoutes.Vertex(7.19712476, 2.26996762),
            DreamRoutes.Vertex(7.13111769, 3.02285711),
            DreamRoutes.Vertex(7.05676134, 3.26708824),
            DreamRoutes.Vertex(7.40881547, 4.31982173),
            DreamRoutes.Vertex(7.34899304, 4.57710159),
            DreamRoutes.Vertex(7.86859494, 4.82763210),
            DreamRoutes.Vertex(8.00699053, 4.57113966),
            DreamRoutes.Vertex(8.53381722, 4.25183505),
            DreamRoutes.Vertex(8.22955357, 3.74154992),
            DreamRoutes.Vertex(8.30390992, 3.22170322),
            DreamRoutes.Vertex(8.65159263, 2.56147878),
            DreamRoutes.Vertex(9.09390047, 2.07943706),
            DreamRoutes.Vertex(9.20668598, 1.85770376),
            DreamRoutes.Vertex(9.22887341, 1.66517435),
            DreamRoutes.Vertex(9.32931774, 1.67328222),
            DreamRoutes.Vertex(9.06691291, 2.14467208),
            DreamRoutes.Vertex(8.65059465, 2.59603889),
            DreamRoutes.Vertex(8.89471259, 2.62854824),
            DreamRoutes.Vertex(9.12875940, 2.73367362),
            DreamRoutes.Vertex(9.30493757, 2.76675406),
            DreamRoutes.Vertex(9.46620933, 4.87823488),
            DreamRoutes.Vertex(9.23912728, 5.23802692),
            DreamRoutes.Vertex(9.20309467, 5.45662783),
            DreamRoutes.Vertex(9.12459882, 5.61650079),
            DreamRoutes.Vertex(9.50000000, 5.83456522),
            DreamRoutes.Vertex(9.10333909, 5.61638830),
            DreamRoutes.Vertex(9.18794228, 5.44903915),
            DreamRoutes.Vertex(9.22057333, 5.24083049),
            DreamRoutes.Vertex(9.45367838, 4.87583800),
            DreamRoutes.Vertex(9.29367869, 2.77186799),
            DreamRoutes.Vertex(9.12564599, 2.74445527),
            DreamRoutes.Vertex(8.88457117, 2.63515913),
            DreamRoutes.Vertex(8.64213995, 2.61952315),
            DreamRoutes.Vertex(8.32479716, 3.20616242),
            DreamRoutes.Vertex(8.26375889, 3.72728111),
            DreamRoutes.Vertex(8.54131611, 4.26773063),
            DreamRoutes.Vertex(8.02771613, 4.58186939),
            DreamRoutes.Vertex(7.87130073, 4.84725712),
            DreamRoutes.Vertex(7.32658774, 4.59773901),
            DreamRoutes.Vertex(6.93461451, 5.36757109),
            DreamRoutes.Vertex(6.61524766, 5.72664492),
            DreamRoutes.Vertex(6.15555252, 5.38258405),
            DreamRoutes.Vertex(6.12881797, 5.39631638),
            DreamRoutes.Vertex(5.98449074, 5.57748438),
            DreamRoutes.Vertex(5.79369431, 5.96873010),
            DreamRoutes.Vertex(5.40372406, 6.99240679),
            DreamRoutes.Vertex(5.24937490, 8.18004066),
            DreamRoutes.Vertex(4.98876221, 8.33482565),
            DreamRoutes.Vertex(5.23957785, 8.16821200),
            DreamRoutes.Vertex(5.39332261, 6.98401337),
            DreamRoutes.Vertex(5.81592391, 5.87560644),
            DreamRoutes.Vertex(5.97259232, 5.56415010),
            DreamRoutes.Vertex(6.13501667, 5.36324458),
            DreamRoutes.Vertex(6.26738222, 5.42258691),
            DreamRoutes.Vertex(6.46526289, 4.67347013),
            DreamRoutes.Vertex(6.46025192, 4.63556130),
            DreamRoutes.Vertex(6.36298425, 4.60898792),
        )
}
