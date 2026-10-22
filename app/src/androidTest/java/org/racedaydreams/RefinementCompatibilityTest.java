package org.racedaydreams;

import android.test.AndroidTestCase;
import java.time.*;
import java.util.Arrays;
import java.util.Collections;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.baseline.BaselineConfidence;
import org.racedaydreams.domain.coaching.PerformanceReassessment;

/** Executes the populated evidence path on API 28 too, where newer Java methods may be absent. */
@SuppressWarnings("deprecation")
public final class RefinementCompatibilityTest extends AndroidTestCase {
    public void testPopulatedPerformanceEvidenceOnSupportedRuntime() {
        LocalDate end=LocalDate.of(2026,10,19);
        RecordedResult old=new RecordedResult("old",PerformanceDistance.FIVE_K,Duration.ofMinutes(30),end.minusDays(60),"Race",ResultSource.OFFICIAL_CHIP,null,"");
        RecordedResult recent=new RecordedResult("recent",PerformanceDistance.FIVE_K,Duration.ofMinutes(27),end.minusDays(3),"Race",ResultSource.OFFICIAL_CHIP,null,"");
        PerformanceReassessment.Review review=PerformanceReassessment.review(Arrays.asList(old,recent),Collections.emptyList(),end,BaselineConfidence.STRONG,1,4,false);
        assertTrue(review.propose());
        assertEquals(PerformanceReassessment.Confidence.STRONG,review.confidence());
        assertEquals(.03,review.fasterFraction(),.000001);
    }
}
