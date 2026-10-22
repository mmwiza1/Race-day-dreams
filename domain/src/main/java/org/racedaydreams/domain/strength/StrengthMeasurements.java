package org.racedaydreams.domain.strength;
import java.util.Locale;
import org.racedaydreams.domain.UnitSystem;
import org.racedaydreams.domain.body.MassUnit;
/** Canonical seconds/metres/kg; display conversion never changes retained evidence. */
public final class StrengthMeasurements {
    private StrengthMeasurements() { }
    public static long seconds(String input) {
        String[] pieces=input.trim().split(":",-1);
        if(pieces.length<1||pieces.length>2)throw new IllegalArgumentException("Enter seconds or minutes:seconds.");
        long value;
        try { if(pieces.length==1)value=Long.parseLong(pieces[0]);else {long m=Long.parseLong(pieces[0]),s=Long.parseLong(pieces[1]);if(m<0||s<0||s>=60)throw new NumberFormatException();value=Math.addExact(Math.multiplyExact(m,60),s);} }
        catch(ArithmeticException|NumberFormatException e){throw new IllegalArgumentException("Enter seconds or minutes:seconds.");}
        if(value<=0||value>43200)throw new IllegalArgumentException("Duration must be positive and at most 12 hours.");return value;
    }
    public static String duration(long seconds){return seconds<60?seconds+" sec":seconds/60+" min"+(seconds%60==0?"":" "+seconds%60+" sec");}
    public static String display(StrengthSession.SetEntry set,MassUnit mass,UnitSystem units) {
        String load=String.format(Locale.UK,"%.1f %s × ",mass.fromKilograms(set.kilograms()),mass.name().toLowerCase(Locale.ROOT));
        if(set.durationSeconds()!=null)return (set.kilograms()>0?load:"")+duration(set.durationSeconds());
        if(set.distanceMetres()!=null)return load+String.format(Locale.UK,"%.1f %s",units==UnitSystem.IMPERIAL?set.distanceMetres()/0.9144:set.distanceMetres(),units==UnitSystem.IMPERIAL?"yd":"m");
        return (set.kilograms()>0?load:"")+set.reps()+" reps";
    }
}
