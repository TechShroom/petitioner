/*
 * Copyright (c) TechShroom <https://techshroom.com>
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.techshroom.petitioner.core;

import com.techshroom.petitioner.core.internal.HttpEncoding;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A wrapper for a {@link SortedMap} with case-insensitive {@link String} keys.
 */
public class HttpHeaderMap implements Iterable<Map.Entry<String, String>> {

    /**
     * {@return a new builder for creating a header map}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@return an empty header map}
     *
     * @apiNote This MAY be a singleton value, but that is not guaranteed
     */
    public static HttpHeaderMap empty() {
        return new HttpHeaderMap(Collections.emptySortedMap());
    }

    /**
     * Creates a new header map from the given map.
     *
     * @param headerMap the map of header entries
     * @return the new header map
     */
    public static HttpHeaderMap from(Map<String, List<String>> headerMap) {
        return builder().putAll(headerMap).build();
    }

    public static final class Builder {
        private final SortedMap<String, List<String>> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        private Builder() {
        }

        public Builder put(String key, String value) {
            return put(key, List.of(value));
        }

        public Builder put(String key, List<String> value) {
            headerMap.put(key, List.copyOf(value));
            return this;
        }

        public Builder add(String key, String value) {
            headerMap.compute(key, (k, oldValues) -> {
                var values = oldValues == null
                    ? new String[1]
                    : oldValues.toArray(new String[oldValues.size() + 1]);
                values[values.length - 1] = value;
                return List.of(values);
            });
            return this;
        }

        public Builder putAll(Map<String, List<String>> entries) {
            // To copy each value map, iterate and call put for each one
            entries.forEach(this::put);
            return this;
        }

        public Builder putAll(HttpHeaderMap entries) {
            return putAll(entries.headerMap);
        }

        public HttpHeaderMap build() {
            if (headerMap.isEmpty()) {
                return empty();
            }
            headerMap.forEach((k, value) -> {
                if (!HttpEncoding.isValidName(k)) {
                    throw new IllegalStateException("Invalid key/name provided: " + k);
                }
                for (String v : value) {
                    if (!HttpEncoding.isValidValue(v)) {
                        throw new IllegalStateException("Invalid value provided: " + v);
                    }
                }
            });
            return new HttpHeaderMap(headerMap);
        }
    }

    private final SortedMap<String, List<String>> headerMap;

    private HttpHeaderMap(SortedMap<String, List<String>> headerMap) {
        this.headerMap = headerMap;
    }

    /**
     * Extracts either no value, or a single value, from the given header key.
     *
     * @param key the key to retrieve the value for
     * @return the value, if there is one
     * @throws IllegalStateException if there is more than one value
     */
    public @Nullable String value(String key) {
        var values = headerMap.get(key);
        if (values == null) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalStateException("More than one value for '" + key + "'");
        }
        return values.get(0);
    }

    /**
     * Extracts a single value from the given header key.
     *
     * @param key the key to retrieve the value for
     * @return the value
     * @throws IllegalStateException if there is not exactly one value
     */
    public String requireValue(String key) {
        var value = value(key);
        if (value == null) {
            throw new IllegalStateException("No value for '" + key + "'");
        }
        return value;
    }

    /**
     * Extracts all known values for the given key.
     *
     * @param key the key to retrieve the value for
     * @return the values, or an empty list if there is none
     */
    public List<String> values(String key) {
        return Objects.requireNonNullElse(
            headerMap.get(key),
            List.of()
        );
    }

    /**
     * Extracts all known values for the given key, throwing if there are none.
     *
     * @param key the key to retrieve the value for
     * @return the values
     * @throws IllegalStateException if there are no values
     */
    public List<String> requireValues(String key) {
        var value = headerMap.get(key);
        if (value == null) {
            throw new IllegalStateException("No value for '" + key + "'");
        }
        return value;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return new Iterator<>() {
            private final Iterator<Map.Entry<String, List<String>>> parent = headerMap.entrySet().iterator();
            private Map.Entry<String, Iterator<String>> currentParentEntry;
            private Map.Entry<String, String> nextEntry;

            @Override
            public boolean hasNext() {
                if (nextEntry != null) {
                    return true;
                }
                // Load next parent entry iterator if needed
                while (currentParentEntry == null) {
                    if (!parent.hasNext()) {
                        return false;
                    }
                    var next = parent.next();
                    var nextEntry = Map.entry(next.getKey(), next.getValue().iterator());
                    // Ensure invariant of having next value holds, otherwise keep looping for one
                    if (nextEntry.getValue().hasNext()) {
                        currentParentEntry = nextEntry;
                    }
                }
                initializeNextEntry();
                return true;
            }

            private void initializeNextEntry() {
                var nextKey = currentParentEntry.getKey();
                var nextIter = currentParentEntry.getValue();
                var nextValue = nextIter.next();
                if (!nextIter.hasNext()) {
                    currentParentEntry = null;
                }
                nextEntry = Map.entry(nextKey, nextValue);
            }

            @Override
            public Map.Entry<String, String> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                var next = this.nextEntry;
                this.nextEntry = null;
                return next;
            }
        };
    }

    public Builder toBuilder() {
        return builder().putAll(this);
    }

    @Override
    public String toString() {
        String headersJoined = headerMap.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(", "));
        return "Headers[" + headersJoined + "]";
    }
}
