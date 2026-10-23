package org.racedaydreams.coach

import android.test.AndroidTestCase
import org.racedaydreams.domain.support.DreamBasemap
import org.racedaydreams.domain.support.DreamRoutes

/** Parses every shipped extract on each API, independently of screenshot assertions. */
class DreamBasemapAssetTest: AndroidTestCase() {
    fun testEveryGeographicCityHasBoundedReadableBundledContext() {
        val assets=context.assets.list("dream-basemaps")!!.toSet()
        assertEquals(5,assets.size)
        for(city in DreamRoutes.routes()) {
            val route=DreamRoutes.currentGeometry(city)
            assertTrue(assets.contains("${route.id()}.rddmap"))
            val map=context.assets.open("dream-basemaps/${route.id()}.rddmap").use { DreamBasemap.read(it,route.id()) }
            assertEquals(route.id(),map.routeId())
            assertTrue(map.features().size>100)
            for(kind in DreamBasemap.Kind.values())assertTrue(map.features().any{it.kind()==kind})
            assertTrue(DreamRoutes.attribution(route).contains("OpenStreetMap"))
        }
    }
}
