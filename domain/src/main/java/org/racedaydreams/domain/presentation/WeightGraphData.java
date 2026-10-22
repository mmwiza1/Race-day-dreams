package org.racedaydreams.domain.presentation;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.body.*;
/** Retained points only. Connections are visual guides, never added measurements. */
public record WeightGraphData(List<BodyWeight> points, double minimum, double maximum, MassUnit unit) {
    public static WeightGraphData of(List<BodyWeight> records, LocalDate today, String range, MassUnit unit) {
        LocalDate first=switch(range){case "Week"->today.minusWeeks(1);case "Month"->today.minusMonths(1);case "3 Months"->today.minusMonths(3);case "Year"->today.minusYears(1);default->LocalDate.MIN;};
        var points=new ArrayList<BodyWeight>();for(var w:records){var d=w.at().atOffset(w.offset()).toLocalDate();if(!d.isBefore(first)&&!d.isAfter(today))points.add(w);}
        points.sort(Comparator.comparing(BodyWeight::at).thenComparing(BodyWeight::id));
        double min=points.stream().mapToDouble(w->unit.fromKilograms(w.kilograms())).min().orElse(0),max=points.stream().mapToDouble(w->unit.fromKilograms(w.kilograms())).max().orElse(0);
        double pad=Math.max(unit==MassUnit.KG?.5:1,(max-min)*.15);
        return new WeightGraphData(Collections.unmodifiableList(points),min-pad,max+pad,unit);
    }
    public double x(int i){if(points.size()<2)return .5;long span=points.get(points.size()-1).at().toEpochMilli()-points.get(0).at().toEpochMilli();return span==0?.5:(points.get(i).at().toEpochMilli()-points.get(0).at().toEpochMilli())/(double)span;}
    public double y(int i){return 1-(unit.fromKilograms(points.get(i).kilograms())-minimum)/(maximum-minimum);}
    public BodyWeight nearest(double x,double y){if(points.isEmpty())return null;int best=0;for(int i=1;i<points.size();i++)if(Math.pow(x(i)-x,2)+Math.pow(y(i)-y,2)<Math.pow(x(best)-x,2)+Math.pow(y(best)-y,2))best=i;return points.get(best);}
    public BodyWeight nearest(double fraction){if(points.isEmpty())return null;int best=0;for(int i=1;i<points.size();i++)if(Math.abs(x(i)-fraction)<Math.abs(x(best)-fraction))best=i;return points.get(best);}
}
