package org.racedaydreams.domain.health;

import java.time.*;
import java.util.*;

/** Source-specific personal context, never a medical or readiness score. */
public final class HealthIntelligence {
    private HealthIntelligence() {}
    public enum Metric { SLEEP_MINUTES, RESTING_HR, HRV_RMSSD, STEPS }
    public record Observation(String id, Metric metric, LocalDate date, Instant start, Instant end,
                              double value, String source, Instant importedAt) {
        public Observation {
            Objects.requireNonNull(id);Objects.requireNonNull(metric);Objects.requireNonNull(date);
            Objects.requireNonNull(start);Objects.requireNonNull(end);Objects.requireNonNull(importedAt);
            if(id.isBlank()||source==null||source.isBlank()||end.isBefore(start)||!Double.isFinite(value)||value<0)
                throw new IllegalArgumentException("Invalid health observation");
            if((metric==Metric.RESTING_HR||metric==Metric.HRV_RMSSD)&&value==0)throw new IllegalArgumentException("Missing is not zero");
        }
    }
    public record Trend(Metric metric, String source, Double recent, Double baseline,
                        int recentDays, int referenceDays, String interpretation, String limitation) {}
    /** Latest source wins; comparable-source evidence only. Reference excludes recent seven days. */
    public static List<Trend> evaluate(List<Observation> input, LocalDate today) {
        List<Trend> out=new ArrayList<>();
        for(Metric m:Metric.values()) {
            List<Observation> rows=input.stream().filter(x->x.metric()==m&&!x.date().isAfter(today)&&!x.date().isBefore(today.minusDays(34))).sorted(Comparator.comparing(Observation::start).reversed().thenComparing(Observation::source)).toList();
            if(rows.isEmpty()){out.add(new Trend(m,"Unavailable",null,null,0,0,"Insufficient evidence","Optional data is missing."));continue;}
            String source=rows.get(0).source();Map<LocalDate,Observation> days=new TreeMap<>();
            // One comparable-source observation per day. Never sum mirrored raw records.
            rows.stream().filter(x->x.source().equals(source)).forEach(x->days.putIfAbsent(x.date(),x));
            List<Double> recent=new ArrayList<>(),base=new ArrayList<>();
            days.forEach((date,x)->{if(!date.isBefore(today.minusDays(6)))recent.add(x.value());else base.add(x.value());});
            Double r=average(recent),b=average(base);String state="Insufficient evidence";
            if(recent.size()>=4&&base.size()>=14&&b!=null&&b>0){
                double ratio=r/b;double spread=base.stream().mapToDouble(x->Math.abs(x-b)).average().orElse(0);
                double threshold=Math.max(spread*1.5,b*(m==Metric.HRV_RMSSD?0.15:m==Metric.STEPS?0.25:0.08));
                state=Math.abs(r-b)<=threshold?"Within recent personal range":r>b?"Above recent personal baseline":"Below recent personal baseline";
            }
            out.add(new Trend(m,source,r,b,recent.size(),base.size(),state,
                "7-day trend / preceding 28-day reference; at least 4 and 14 measured days required. Source changes are not combined. Supporting context, not diagnosis."));
        }
        return List.copyOf(out);
    }
    private static Double average(List<Double> x){return x.isEmpty()?null:x.stream().mapToDouble(Double::doubleValue).average().orElseThrow();}
}
