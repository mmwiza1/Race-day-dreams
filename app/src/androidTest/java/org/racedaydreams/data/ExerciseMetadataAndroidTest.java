package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.racedaydreams.domain.strength.ExerciseCatalog;
import org.racedaydreams.domain.strength.ExerciseMetadata;
import org.racedaydreams.domain.strength.ExerciseMetadataCatalog;

import java.util.Collections;
import java.util.EnumSet;

/** Runs registry initialization on every supported Android runtime, including API 28. */
@SuppressWarnings("deprecation")
public final class ExerciseMetadataAndroidTest extends AndroidTestCase {
    public void testAllCanonicalMetadataLoadsOnTheSupportedAndroidRuntime() {
        assertEquals(
                ExerciseCatalog.definitions().size(), ExerciseMetadataCatalog.entries().size());
        for (ExerciseCatalog.Definition definition : ExerciseCatalog.definitions()) {
            ExerciseMetadata metadata = ExerciseMetadataCatalog.require(definition.exercise().id());
            assertEquals(definition.exercise().id(), metadata.exerciseId());
            assertTrue(metadata.roles().contains(metadata.primaryRole()));
            assertFalse(metadata.primaryAreas().isEmpty());
        }
        ExerciseMetadata squat = ExerciseMetadataCatalog.require("back-squat");
        assertFalse(squat.equipment().availableWith(Collections.emptySet()));
        assertTrue(
                squat.equipment()
                        .availableWith(
                                EnumSet.of(
                                        ExerciseMetadata.Equipment.BARBELL,
                                        ExerciseMetadata.Equipment.SQUAT_RACK)));
        assertNull(ExerciseMetadataCatalog.find("unknown-custom-exercise"));
    }
}
