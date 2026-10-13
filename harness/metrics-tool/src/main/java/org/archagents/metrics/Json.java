package org.archagents.metrics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/** Minimal JSON writer (maps, collections, strings, numbers, booleans, null). */
final class Json {

    private Json() {
    }

    static Double round(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return null;
        }
        return BigDecimal.valueOf(d).setScale(6, RoundingMode.HALF_EVEN).doubleValue();
    }

    static String write(Object o) {
        StringBuilder sb = new StringBuilder();
        write(o, sb);
        return sb.toString();
    }

    private static void write(Object o, StringBuilder sb) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof String s) {
            str(s, sb);
        } else if (o instanceof Number || o instanceof Boolean) {
            sb.append(o);
        } else if (o instanceof Map<?, ?> m) {
            sb.append('{');
            Iterator<? extends Map.Entry<?, ?>> it = m.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> e = it.next();
                str(String.valueOf(e.getKey()), sb);
                sb.append(':');
                write(e.getValue(), sb);
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append('}');
        } else if (o instanceof Collection<?> c) {
            sb.append('[');
            Iterator<?> it = c.iterator();
            while (it.hasNext()) {
                write(it.next(), sb);
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append(']');
        } else {
            str(o.toString(), sb);
        }
    }

    private static void str(String s, StringBuilder sb) {
        sb.append('"');
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        sb.append('"');
    }
}
