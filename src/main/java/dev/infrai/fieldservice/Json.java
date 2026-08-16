package dev.infrai.fieldservice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {}

    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        append(out, value);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static void append(StringBuilder out, Object value) {
        if (value == null) out.append("null");
        else if (value instanceof String text) string(out, text);
        else if (value instanceof Number || value instanceof Boolean) out.append(value);
        else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean comma = false;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (comma) out.append(',');
                string(out, entry.getKey().toString());
                out.append(':');
                append(out, entry.getValue());
                comma = true;
            }
            out.append('}');
        } else if (value instanceof Iterable<?> values) {
            out.append('[');
            boolean comma = false;
            for (Object item : values) {
                if (comma) out.append(',');
                append(out, item);
                comma = true;
            }
            out.append(']');
        } else throw new IllegalArgumentException("Cannot encode " + value.getClass());
    }

    private static void string(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }

    static Map<String, Object> readObject(String source) {
        Object value = new Parser(source).parse();
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("JSON envelope must be an object");
        @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) map;
        return result;
    }

    private static final class Parser {
        private final String source;
        private int index;

        Parser(String source) { this.source = source; }

        Object parse() {
            Object value = value();
            whitespace();
            if (index != source.length()) fail("trailing content");
            return value;
        }

        private Object value() {
            whitespace();
            if (index >= source.length()) return fail("missing value");
            return switch (source.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            index++;
            Map<String, Object> map = new LinkedHashMap<>();
            whitespace();
            if (take('}')) return map;
            do {
                whitespace();
                if (index >= source.length() || source.charAt(index) != '"') fail("object key");
                String key = string();
                whitespace();
                if (!take(':')) fail("colon");
                map.put(key, value());
                whitespace();
                if (take('}')) return map;
            } while (take(','));
            return fail("object separator");
        }

        private List<Object> array() {
            index++;
            List<Object> list = new ArrayList<>();
            whitespace();
            if (take(']')) return list;
            do {
                list.add(value());
                whitespace();
                if (take(']')) return list;
            } while (take(','));
            return fail("array separator");
        }

        private String string() {
            index++;
            StringBuilder out = new StringBuilder();
            while (index < source.length()) {
                char c = source.charAt(index++);
                if (c == '"') return out.toString();
                if (c != '\\') { out.append(c); continue; }
                if (index >= source.length()) fail("escape");
                char escaped = source.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (index + 4 > source.length()) fail("unicode escape");
                        out.append((char) Integer.parseInt(source.substring(index, index + 4), 16));
                        index += 4;
                    }
                    default -> fail("escape character");
                }
            }
            return fail("string terminator");
        }

        private Object number() {
            int start = index;
            while (index < source.length() && "-+0123456789.eE".indexOf(source.charAt(index)) >= 0) index++;
            if (start == index) return fail("number");
            String text = source.substring(start, index);
            return text.contains(".") || text.contains("e") || text.contains("E")
                ? Double.valueOf(text) : Long.valueOf(text);
        }

        private Object literal(String text, Object value) {
            if (!source.startsWith(text, index)) return fail("literal");
            index += text.length();
            return value;
        }

        private void whitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++;
        }

        private boolean take(char expected) {
            if (index < source.length() && source.charAt(index) == expected) { index++; return true; }
            return false;
        }

        private <T> T fail(String expected) {
            throw new IllegalArgumentException("Invalid JSON at " + index + ": expected " + expected);
        }
    }
}
