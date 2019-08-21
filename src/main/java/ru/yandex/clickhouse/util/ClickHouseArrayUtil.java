package ru.yandex.clickhouse.util;

import ru.yandex.clickhouse.ClickHouseUtil;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 */
public class ClickHouseArrayUtil {
    private ClickHouseArrayUtil() {
    }

    public static String arrayToString(Object object) {
        return arrayToString(object, true);
    }

    /**
     * @param object         the object to convert to ClickHouse-string representation
     * @param explicitEscape enable or disable elements escaping (works only for non-primitive values)
     * @return string representation of an object
     */
    public static String arrayToString(Object object, Boolean explicitEscape) {
        if (!object.getClass().isArray()) {
            throw new IllegalArgumentException("Object must be array");
        }
        if (object.getClass().getComponentType().isPrimitive()) {
            return primitiveArrayToString(object);
        } else {
            return toString((Object[]) object, explicitEscape);
        }
    }


    private static String primitiveArrayToString(Object array) {
        if (array instanceof int[]) {
            return Arrays.toString((int[])array);
        } else if (array instanceof long[]) {
            return Arrays.toString((long[]) array);
        } else if (array instanceof float[]) {
            return Arrays.toString((float[])array);
        } else if (array instanceof double[]) {
            return Arrays.toString((double[]) array);
        } else if (array instanceof char[]) {
            return toString((char[]) array);
        } else if (array instanceof byte[]) {
            return toString((byte[]) array);
        } else if (array instanceof short[]) {
            return Arrays.toString((short[]) array);
        } else {
            throw new IllegalArgumentException("Wrong primitive type: " + array.getClass().getComponentType());
        }
    }

    public static String toString(byte[] values) {
        ArrayBuilder builder = new ArrayBuilder(false, true);
        for (byte value : values) {
            builder.append(value);
        }
        return builder.build();
    }

    public static String toString(char[] values) {
        ArrayBuilder builder = new ArrayBuilder(true, true);
        for (char value : values) {
            builder.append(value);
        }
        return builder.build();
    }


    public static String toString(Object[] values) {
        return toString(values, true);
    }

    public static String toString(Object[] values, Boolean explicitEscape) {
        if (values.length > 0 && values[0] != null && (values[0].getClass().isArray() || values[0] instanceof Collection)) {
            // quote is false to avoid escaping inner '['
            ArrayBuilder builder = new ArrayBuilder(false, true);
            for (Object value : values) {
                if (value instanceof Collection) {
                    Object[] objects = ((Collection) value).toArray();
                    builder.append(toString(objects, explicitEscape));
                } else {
                    builder.append(arrayToString(value, explicitEscape));
                }
            }
            return builder.build();
        }
        ArrayBuilder builder = new ArrayBuilder(needQuote(values), explicitEscape);
        for (Object value : values) {
            builder.append(value);
        }
        return builder.build();
    }

    public static String toString(Collection collection) {
        return toString(collection, true);
    }

    /**
     * Convert collection to its ClickHouse-string representation.
     *
     * @param collection the collection to transform
     * @param escape     enable or disable escaping of the collection elements
     * @return string representation of a collection
     */
    public static String toString(Collection collection, boolean escape) {
        return toString(collection.toArray(), escape);
    }

    private static boolean needQuote(Object[] objects) {
        Object o = null;
        for (Object u : objects) {
            if (u != null) {
                o = u;
                break;
            }
        }
        return objects.length == 0 || !(o instanceof Number);
    }

    private static class ArrayBuilder {
        private final StringBuilder builder = new StringBuilder();
        private final boolean quote;
        private final boolean explicitEscape;
        private int size = 0;
        private boolean built = false;

        private ArrayBuilder(boolean quote, boolean explicitEscape) {
            this.quote = quote;
            this.explicitEscape = explicitEscape;
            builder.append('[');
        }

        private ArrayBuilder append(Object value) {
            if (built) {
                throw new IllegalStateException("Already built");
            }
            if (size > 0) {
                builder.append(',');
            }
            if (value != null) {
                String serializedValue = value.toString();
                String escapedValue = explicitEscape ? ClickHouseUtil.escape(serializedValue) : serializedValue;
                if (quote) {
                    builder.append('\'');
                    builder.append(escapedValue);
                    builder.append('\'');
                } else {
                    builder.append(serializedValue);
                }
            } else {
                builder.append("NULL");
            }
            size++;
            return this;
        }

        private String build() {
            if (!built) {
                builder.append(']');
                built = false;
            }
            return builder.toString();
        }
    }
}
