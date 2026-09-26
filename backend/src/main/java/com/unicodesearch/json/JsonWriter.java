package com.unicodesearch.json;

import java.util.List;
import java.util.Map;

/**
 * Minimal hand-written JSON serializer. We deliberately avoid pulling in
 * Jackson/Gson so this backend compiles and runs with ONLY the JDK --
 * no build tool or internet access required, which keeps the project
 * fully self-contained for a student environment.
 *
 * Supports: String, Number, Boolean, null, Map<String,?>, List<?>, and
 * nested combinations of these -- which is everything our API needs.
 */
public final class JsonWriter {

    private JsonWriter() {}

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(s, sb);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Map) {
            writeMap((Map<String, ?>) value, sb);
        } else if (value instanceof List) {
            writeList((List<?>) value, sb);
        } else {
            writeString(value.toString(), sb);
        }
    }

    private static void writeMap(Map<String, ?> map, StringBuilder sb) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(entry.getKey(), sb);
            sb.append(':');
            writeValue(entry.getValue(), sb);
        }
        sb.append('}');
    }

    private static void writeList(List<?> list, StringBuilder sb) {
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(',');
            first = false;
            writeValue(item, sb);
        }
        sb.append(']');
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c); // Unicode chars (Telugu/Hindi/Tamil/Bengali) pass through as UTF-8 on write
                    }
            }
        }
        sb.append('"');
    }
}
