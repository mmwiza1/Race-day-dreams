package org.racedaydreams.domain.activity;
/** Friendly Health Connect origin labels and compatibility for obsolete saved selections. */
public final class ActivitySources {
    private ActivitySources() { }
    public static String normalizeLegacyPreference(String ignored){return "HEALTH_CONNECT";}
    public static String friendly(SourceIdentity source){
        if("health-connect".equals(source.provider())){
            if("com.sec.android.app.shealth".equals(source.originPackage()))return "Samsung Health";
            if("com.fitbit.FitbitMobile".equals(source.originPackage()))return "Fitbit";
            return "Health Connect";
        }
        return "manual".equals(source.provider())?"Manual entry":"Activity provider";
    }
}
