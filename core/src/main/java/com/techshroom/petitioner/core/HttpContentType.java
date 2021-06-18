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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public record HttpContentType(
    String mimeType,
    @Nullable String charset
) {
    public static HttpContentType of(String mimeType, @Nullable Charset charset) {
        return new HttpContentType(mimeType, charset == null ? null : charset.name());
    }

    /**
     * Attempt to resolve the charset name to a {@link Charset} instance.
     *
     * @return the resolved instance
     * @throws UnsupportedCharsetException if this JVM does not know of a charset named {@link #charset()}
     */
    public @Nullable Charset resolveCharset() {
        if (charset == null) {
            return null;
        }
        return Charset.forName(charset);
    }
}
