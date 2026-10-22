package org.racedaydreams.domain.strength;
import java.time.LocalDate;
import java.util.Objects;
/** Independent historical evidence, not a fabricated completed workout. */
public record HistoricalStrengthRecord(String id, StrengthExercise exercise, double kilograms, int reps,
        LocalDate date, DatePrecision precision, String notes, Long durationSeconds, Double distanceMetres) {
    public HistoricalStrengthRecord(String id,StrengthExercise exercise,double kilograms,int reps,LocalDate date,DatePrecision precision,String notes) { this(id,exercise,kilograms,reps,date,precision,notes,null,null); }
    public enum DatePrecision { EXACT, APPROXIMATE, UNKNOWN }
    public HistoricalStrengthRecord {
        Objects.requireNonNull(exercise); Objects.requireNonNull(precision);
        new StrengthSession.ExerciseSets(exercise,java.util.Collections.singletonList(new StrengthSession.SetEntry(id,reps,kilograms,null,false,durationSeconds,distanceMetres)));
        if(notes!=null&&notes.length()>10000)throw new IllegalArgumentException("Notes are too long.");
        if (id == null || id.trim().isEmpty() || !Double.isFinite(kilograms) || kilograms < 0 || kilograms > 1500 || reps < 0 || reps > 500
            || (precision == DatePrecision.UNKNOWN) != (date == null)) throw new IllegalArgumentException("Check historical record and date precision.");
    }
}
