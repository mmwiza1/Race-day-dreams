package org.racedaydreams.domain.body;

import org.racedaydreams.domain.settings.PrimaryFocus;

import java.time.*;
import java.util.*;

public final class BodyGuidanceTestSuite {
    private static int checks;
    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

    public static void main(String[] args) {
        var manual = weight("manual", 87, NOW.minusSeconds(86400), "manual");
        var imported = weight("hc", 90, NOW.minusSeconds(1), "health-connect");
        var basis = ProteinGuidance.Basis.from(manual);
        for (var focus : PrimaryFocus.values()) {
            check(
                    !ProteinGuidance.evaluate(false, basis, List.of(manual), focus, NOW)
                            .available(),
                    "Disabled is optional");
            check(
                    !ProteinGuidance.evaluate(true, null, List.of(manual, imported), focus, NOW)
                            .available(),
                    "No automatic provider authority");
            var a = ProteinGuidance.evaluate(true, basis, List.of(manual, imported), focus, NOW);
            check(
                    a.available() && a.basis().equals(manual),
                    "Explicit manual choice remains basis despite newer provider");
            check(
                    a.minimumGrams() <= a.maximumGrams()
                            && a.minimumGrams() % 5 == 0
                            && a.maximumGrams() % 5 == 0,
                    "Approximate bounded display");
            check(
                    !ProteinGuidance.evaluate(true, basis, List.of(imported), focus, NOW)
                            .available(),
                    "Deleted basis never silently replaced");
            check(
                    !ProteinGuidance.evaluate(
                                    true,
                                    basis,
                                    List.of(weight("manual", 88, manual.at(), "manual")),
                                    focus,
                                    NOW)
                            .available(),
                    "Changed source value needs explicit review");
            for (int kg = 30; kg <= 250; kg++) {
                var row = weight("weight", kg, NOW, "manual");
                var advice =
                        ProteinGuidance.evaluate(
                                true, ProteinGuidance.Basis.from(row), List.of(row), focus, NOW);
                check(advice.available(), "Supported adult example");
                check(
                        advice.maximumGrams() <= kg * 2 + 2.5
                                && advice.minimumGrams() >= kg * 1.2 - 2.5,
                        "No extreme range or fake precision");
            }
        }
        var general = ProteinGuidance.evaluate(true, basis, List.of(manual), null, NOW);
        near(1.2, general.minimumPerKg());
        near(2.0, general.maximumPerKg());
        check(
                general.explanation().contains("general training"),
                "Unknown focus is not called running or strength");
        var strength =
                ProteinGuidance.evaluate(true, basis, List.of(manual), PrimaryFocus.STRENGTH, NOW);
        eq(120, strength.minimumGrams());
        eq(175, strength.maximumGrams());
        var run = ProteinGuidance.evaluate(true, basis, List.of(manual), PrimaryFocus.RUNNING, NOW);
        eq(105, run.minimumGrams());
        eq(140, run.maximumGrams());
        for (long days : new long[] {-1, 91, 365}) {
            var row = weight("time", 87, NOW.minus(Duration.ofDays(days)), "manual");
            check(
                    !ProteinGuidance.evaluate(
                                    true,
                                    ProteinGuidance.Basis.from(row),
                                    List.of(row),
                                    PrimaryFocus.STRENGTH,
                                    NOW)
                            .available(),
                    "Future/stale observations not used");
        }
        for (double kg : new double[] {10, 29.9, 250.1, 400}) {
            var row = weight("outside", kg, NOW, "manual");
            check(
                    !ProteinGuidance.evaluate(
                                    true,
                                    ProteinGuidance.Basis.from(row),
                                    List.of(row),
                                    PrimaryFocus.HYBRID,
                                    NOW)
                            .available(),
                    "Outside general applicability no numbers");
        }
        near(1.778, HeightUnit.INCHES.toMetres(70));
        near(70, HeightUnit.INCHES.fromMetres(1.778));
        near(178, HeightUnit.CENTIMETRES.fromMetres(1.78));
        var h = new BodyHeight("h", 1.78, NOW, ZoneOffset.ofHours(1), "manual", "h", null, null);
        eq("manual", h.provider());
        eq(null, h.importedAt());
        eq(ZoneOffset.ofHours(1), h.offset());
        rejects(
                () ->
                        new BodyHeight(
                                "h", Double.NaN, NOW, ZoneOffset.UTC, "manual", "h", null, null));
        rejects(() -> HeightUnit.INCHES.toMetres(-1));
        rejects(
                () ->
                        ProteinGuidance.evaluate(
                                true,
                                basis,
                                List.of(manual, weight("manual", 88, manual.at(), "manual")),
                                PrimaryFocus.STRENGTH,
                                NOW));
        check(
                ProteinGuidance.sources().contains("1.2–2.0"),
                "Documented bounded sports-nutrition basis");
        System.out.println("BodyGuidanceTestSuite: " + checks + " checks passed.");
    }

    private static BodyWeight weight(String id, double kg, Instant at, String provider) {
        return new BodyWeight(
                id,
                kg,
                at,
                ZoneOffset.UTC,
                provider,
                id,
                provider.equals("manual") ? null : "provider.package");
    }

    private static void check(boolean v, String why) {
        checks++;
        if (!v) throw new AssertionError(why);
    }

    private static void eq(Object a, Object b) {
        check(Objects.equals(a, b), a + " != " + b);
    }

    private static void near(double a, double b) {
        check(Math.abs(a - b) < 0.00000001, "Unit conversion");
    }

    private static void rejects(Runnable r) {
        checks++;
        try {
            r.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected rejection");
    }
}
