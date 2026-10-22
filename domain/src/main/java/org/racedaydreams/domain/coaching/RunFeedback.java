package org.racedaydreams.domain.coaching;

import java.util.List;

/** Null score means there is no prescription or insufficient independent execution evidence. */
public record RunFeedback(Integer score, String label, List<Component> components, List<String> wentWell,
        List<String> differed, String learned, String nextStep) {
    public record Component(String name, Double fraction, String explanation) { }
    public RunFeedback {
        components = List.copyOf(components); wentWell = List.copyOf(wentWell); differed = List.copyOf(differed);
    }
}
