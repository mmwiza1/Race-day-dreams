package org.racedaydreams.coach

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.support.DreamRoutes

/** Bundled visual resources and identity must match the retained assignment, including old IDs. */
@RunWith(AndroidJUnit4::class)
class DreamRoutePresentationTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun everyCurrentCityHasFourBoundedDecodableOriginalImages() {
        val all = mutableSetOf<String>()
        for (city in DreamRoutes.routes()) {
            val spec = dreamCityPresentation(DreamRoutes.currentGeometry(city))
            assertEquals(3, spec.landmarks.size)
            for (asset in listOf(spec.hero) + spec.landmarks.map { it.asset }) {
                assertTrue("Duplicate city asset $asset", all.add(asset))
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.assets.open("dream-art/$asset.webp").use { BitmapFactory.decodeStream(it, null, bounds) }
                assertTrue("Missing or enormous image $asset", bounds.outWidth in 320..1200 && bounds.outHeight in 320..400)
                assertEquals("image/webp", bounds.outMimeType)
            }
        }
        assertEquals(20, all.size)
    }

    @Test fun annualParisHasAnnualIdentityAndVerifiedLandmarks() {
        val spec = dreamCityPresentation(DreamRoutes.forId("paris-2026-geo-v1"))
        assertEquals("Marathon de Paris", spec.course)
        assertEquals("2026 course", spec.edition)
        assertEquals(listOf("Eiffel Tower", "Château de Vincennes", "Louvre"), spec.landmarks.map { it.label })
        assertFalse(spec.course.contains("Olympic"))
    }

    @Test fun oldParisIdentityAndLandmarksAreNotReinterpreted() {
        val old = DreamRoutes.Assignment("paris-2024-olympic-geo-v1", 7000.0, 7190.0)
        val spec = dreamCityPresentation(DreamRoutes.forId(old.routeId()))
        assertEquals("Paris 2024 Olympic marathon course", spec.course)
        assertEquals("Archived Olympic course · 2024", spec.edition)
        assertEquals(listOf("Eiffel Tower", "Louvre"), spec.landmarks.map { it.label })
        assertEquals("paris-2024-olympic-geo-v1", old.routeId())
        assertEquals(7000.0, old.startMetres(), 0.0)
        assertEquals(7190.0, old.coveredMetres(), 0.0)
    }

    @Test fun historicalAndArchivedGeometryKeepTheirEdition() {
        assertEquals("2014 course", dreamCityPresentation(DreamRoutes.forId("berlin-2014-geo-v1")).edition)
        assertEquals("Historical course · edition unspecified", dreamCityPresentation(DreamRoutes.forId("london-historical-geo-v1")).edition)
        for (city in DreamRoutes.routes()) {
            val spec = dreamCityPresentation(city)
            assertFalse(spec.geographic)
            assertEquals("Archived course-inspired drawing", spec.edition)
            assertTrue(spec.course.contains("course-inspired"))
        }
    }

    @Test fun currentPosterLabelsBelongToTheSavedCourseContext() {
        for (city in DreamRoutes.routes()) {
            val route = DreamRoutes.currentGeometry(city)
            val retained = DreamRoutes.landmarks(route).map { it.label() }.toSet()
            assertTrue(retained.containsAll(dreamCityPresentation(route).landmarks.map { it.label }))
        }
    }
}
