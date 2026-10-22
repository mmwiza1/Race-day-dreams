package org.racedaydreams.domain.activity;
import java.time.Duration;
import java.util.*;
import org.racedaydreams.domain.coaching.Immutable;
/** Conservative cross-provider matching. Raw records remain retained; canonical choices carry both identities. */
public final class ProviderReconciliation {
    private ProviderReconciliation() { }
    public enum Preference { HEALTH_CONNECT }
    /** Old backups may carry removed provider preferences. Only Health Connect is supported. */
    public static Preference normalizePreference(String ignoredLegacyValue) { return Preference.HEALTH_CONNECT; }
    public record Canonical(ActivityRecord activity,List<SourceIdentity> sources) { }
    /** No speculative cross-provider merging. Retained historical records are not discarded. */
    public static List<Canonical> reconcile(List<ActivityRecord> records,Preference preference) {
        var out=new ArrayList<Canonical>();for(var a:records)out.add(new Canonical(a,Immutable.list(a.source())));
        return Immutable.copyList(out);
    }
    private static boolean samsung(SourceIdentity s) { return "health-connect".equals(s.provider()) && "com.sec.android.app.shealth".equals(s.originPackage()); }
    public static String friendly(SourceIdentity s){if(samsung(s))return "Samsung Health";if("com.fitbit.FitbitMobile".equals(s.originPackage()))return "Fitbit";return "health-connect".equals(s.provider())?"Health Connect":"manual".equals(s.provider())?"Manual entry":"Activity provider";}
}
