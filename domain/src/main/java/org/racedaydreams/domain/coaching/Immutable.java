package org.racedaydreams.domain.coaching;

import java.util.*;

/** API-28-compatible immutable collections, without Java 9/16 collection runtime methods. */
public final class Immutable {
    private Immutable() { }
    @SafeVarargs public static <T> List<T> list(T... values) { return copyList(Arrays.asList(values)); }
    public static <T> List<T> copyList(Collection<T> values) {var result=new ArrayList<>(values);for(T value:result)Objects.requireNonNull(value);return Collections.unmodifiableList(result);}
    @SafeVarargs public static <T> Set<T> set(T... values) { return copySet(Arrays.asList(values)); }
    public static <T> Set<T> copySet(Collection<T> values) {var result=new HashSet<>(values);for(T value:result)Objects.requireNonNull(value);return Collections.unmodifiableSet(result);}
}
