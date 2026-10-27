package org.racedaydreams.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Strict, bounded JSON primitives for the independently versioned gym payloads. */
final class GymJsonSupport {
    static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final int MAX_DEPTH = 20;
    // Includes a finished snapshot's repeated actual evidence as well as the maximum event list.
    private static final int MAX_TOKENS = 1000000;

    private GymJsonSupport() {}

    static JSONObject object(Object... pairs) {
        JSONObject result = new JSONObject();
        try {
            for (int i = 0; i < pairs.length; i += 2)
                result.put((String) pairs[i], pairs[i + 1] == null ? JSONObject.NULL : pairs[i + 1]);
        } catch (JSONException invalid) {
            throw new IllegalArgumentException("Invalid gym JSON value.", invalid);
        }
        return result;
    }

    static void fields(JSONObject json, String... expected) {
        if (json == null) throw invalid("object");
        Set<String> names = new HashSet<>(Arrays.asList(expected));
        if (json.length() != names.size()) throw invalid("required fields");
        for (Iterator<String> keys = json.keys(); keys.hasNext(); )
            if (!names.contains(keys.next())) throw invalid("unknown field");
        for (String name : names) if (!json.has(name)) throw invalid("missing " + name);
    }

    static Object value(JSONObject json, String key) {
        if (!json.has(key)) throw invalid("missing " + key);
        return json.opt(key);
    }

    static boolean isNull(JSONObject json, String key) {
        return value(json, key) == JSONObject.NULL;
    }

    static JSONObject child(JSONObject json, String key) {
        Object value = value(json, key);
        if (!(value instanceof JSONObject)) throw invalid(key + " object");
        return (JSONObject) value;
    }

    static JSONObject row(JSONArray array, int index) {
        Object value = array.opt(index);
        if (!(value instanceof JSONObject)) throw invalid("array object");
        return (JSONObject) value;
    }

    static JSONArray array(JSONObject json, String key, int maximum) {
        Object value = value(json, key);
        if (!(value instanceof JSONArray)) throw invalid(key + " array");
        JSONArray array = (JSONArray) value;
        if (array.length() > maximum) throw invalid(key + " limit");
        return array;
    }

    static String text(JSONObject json, String key, int maximum) {
        Object value = value(json, key);
        if (!(value instanceof String) || ((String) value).length() > maximum)
            throw invalid(key + " text");
        return (String) value;
    }

    static String nullableText(JSONObject json, String key, int maximum) {
        return isNull(json, key) ? null : text(json, key, maximum);
    }

    static boolean bool(JSONObject json, String key) {
        Object value = value(json, key);
        if (!(value instanceof Boolean)) throw invalid(key + " boolean");
        return (Boolean) value;
    }

    /** JSON integer tokens only: no strings, fractional clocks or lossy doubles near Long.MAX_VALUE. */
    static long whole(JSONObject json, String key) {
        Object value = value(json, key);
        if (!(value instanceof Byte)
                && !(value instanceof Short)
                && !(value instanceof Integer)
                && !(value instanceof Long)) throw invalid(key + " integer");
        return ((Number) value).longValue();
    }

    static int integer(JSONObject json, String key) {
        long value = whole(json, key);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw invalid(key + " integer range");
        return (int) value;
    }

    static Integer nullableInteger(JSONObject json, String key) {
        return isNull(json, key) ? null : integer(json, key);
    }

    static Long nullableLong(JSONObject json, String key) {
        return isNull(json, key) ? null : whole(json, key);
    }

    static double number(JSONObject json, String key) {
        Object value = value(json, key);
        if (!(value instanceof Number)) throw invalid(key + " number");
        double number = ((Number) value).doubleValue();
        if (!Double.isFinite(number)) throw invalid(key + " finite number");
        return number;
    }

    static Double nullableNumber(JSONObject json, String key) {
        return isNull(json, key) ? null : number(json, key);
    }

    static <T extends Enum<T>> T choice(JSONObject json, String key, Class<T> type) {
        return Enum.valueOf(type, text(json, key, 64));
    }

    static Instant instant(JSONObject json, String key) {
        try { return Instant.parse(text(json, key, 64)); }
        catch (DateTimeException invalid) { throw new IllegalArgumentException("Invalid gym timestamp: " + key, invalid); }
    }

    static ZoneOffset offset(JSONObject json, String key) {
        try { return ZoneOffset.of(text(json, key, 16)); }
        catch (DateTimeException invalid) { throw new IllegalArgumentException("Invalid gym offset: " + key, invalid); }
    }

    static void version(JSONObject json, int expected) {
        if (integer(json, "version") != expected)
            throw new IllegalArgumentException("Unsupported gym payload version.");
    }

    static String write(JSONObject json) {
        String result = json.toString();
        if (result == null) throw invalid("encoding");
        checkSize(result);
        return result;
    }

    static JSONObject read(String input) {
        checkSize(input);
        // Android's JSONTokener also accepts comments, duplicate keys and other lenient syntax.
        // Validate standard JSON first, with bounded nesting and tokens before allocating its tree.
        new Syntax(input).validate();
        try {
            JSONTokener parser = new JSONTokener(input);
            Object result = parser.nextValue();
            if (!(result instanceof JSONObject) || parser.nextClean() != 0) throw invalid("root object");
            return (JSONObject) result;
        } catch (JSONException invalid) {
            throw new IllegalArgumentException("Malformed gym JSON.", invalid);
        }
    }

    private static void checkSize(String input) {
        if (input == null || input.length() > MAX_BYTES
                || input.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw invalid("payload size");
    }

    static IllegalArgumentException invalid(String detail) {
        return new IllegalArgumentException("Invalid gym JSON: " + detail + ".");
    }

    /** Small bounded syntax gate. This does not replace the domain's semantic validation. */
    private static final class Syntax {
        private final String input;
        private int at;
        private int tokens;

        Syntax(String input) { this.input = input; }

        void validate() {
            whitespace();
            if (at >= input.length() || input.charAt(at) != '{') throw invalid("root object");
            item(0);
            whitespace();
            if (at != input.length()) throw invalid("trailing data");
        }

        private void item(int depth) {
            if (depth > MAX_DEPTH || ++tokens > MAX_TOKENS) throw invalid("structure limit");
            whitespace();
            if (at == input.length()) throw invalid("unexpected end");
            char c = input.charAt(at);
            if (c == '{') {
                at++;
                Set<String> keys = new HashSet<>();
                whitespace();
                if (take('}')) return;
                do {
                    whitespace();
                    int start = at;
                    string();
                    String raw = input.substring(start, at);
                    String key;
                    try { key = (String) new JSONTokener(raw).nextValue(); }
                    catch (JSONException invalid) { throw GymJsonSupport.invalid("object key"); }
                    if (!keys.add(key)) throw invalid("duplicate field");
                    whitespace();
                    if (!take(':')) throw invalid("object separator");
                    item(depth + 1);
                    whitespace();
                    if (take('}')) return;
                } while (take(','));
                throw invalid("object ending");
            }
            if (c == '[') {
                at++;
                whitespace();
                if (take(']')) return;
                do {
                    item(depth + 1);
                    whitespace();
                    if (take(']')) return;
                } while (take(','));
                throw invalid("array ending");
            }
            if (c == '"') { string(); return; }
            if (c == 't') { literal("true"); return; }
            if (c == 'f') { literal("false"); return; }
            if (c == 'n') { literal("null"); return; }
            number();
        }

        private void string() {
            if (!take('"')) throw invalid("quoted string");
            int start = at;
            while (at < input.length()) {
                char c = input.charAt(at++);
                if (at - start > 100000) throw invalid("string limit");
                if (c == '"') return;
                if (c < 0x20) throw invalid("string control character");
                if (c != '\\') continue;
                if (at == input.length()) throw invalid("string escape");
                char escaped = input.charAt(at++);
                if (escaped == 'u') {
                    for (int i = 0; i < 4; i++)
                        if (at == input.length() || Character.digit(input.charAt(at++), 16) < 0)
                            throw invalid("unicode escape");
                } else if ("\"\\/bfnrt".indexOf(escaped) < 0) throw invalid("string escape");
            }
            throw invalid("unfinished string");
        }

        private void number() {
            take('-');
            if (!take('0')) digits();
            if (take('.')) digits();
            if (take('e') || take('E')) {
                if (!take('+')) take('-');
                digits();
            }
        }

        private void digits() {
            int start = at;
            while (at < input.length() && input.charAt(at) >= '0' && input.charAt(at) <= '9') at++;
            if (start == at) throw invalid("number");
        }

        private void literal(String value) {
            if (!input.startsWith(value, at)) throw invalid("literal");
            at += value.length();
        }

        private void whitespace() {
            while (at < input.length() && " \t\r\n".indexOf(input.charAt(at)) >= 0) at++;
        }

        private boolean take(char value) {
            if (at < input.length() && input.charAt(at) == value) { at++; return true; }
            return false;
        }
    }
}
