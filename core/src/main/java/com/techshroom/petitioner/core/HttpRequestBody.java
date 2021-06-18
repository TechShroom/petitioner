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

import com.techshroom.petitioner.core.internal.ByteBufferHttpRequestBody;
import com.techshroom.petitioner.core.io.ReadSession;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.OptionalLong;

/**
 * Represents an HTTP request body.
 */
public interface HttpRequestBody {
    /**
     * {@return a request body for the string}
     *
     * @param content the string to use for the body
     */
    static HttpRequestBody from(String content, String mimeType) {
        return from(content, HttpContentType.of(mimeType, StandardCharsets.UTF_8));
    }

    static HttpRequestBody from(String content, HttpContentType contentType) {
        var buffer = contentType.resolveCharset().encode(content);
        return from(buffer, contentType);
    }

    static HttpRequestBody from(ByteBuffer content, HttpContentType contentType) {
        return new ByteBufferHttpRequestBody(content.asReadOnlyBuffer(), contentType);
    }

    /**
     * {@return the length of content in the body} May be
     * {@link OptionalLong#empty()} if there is no known length.
     */
    default OptionalLong contentLength() {
        return OptionalLong.empty();
    }

    /**
     * {@return the type of content in the body}
     */
    HttpContentType contentType();

    /**
     * Creates a new read session for this body. A request body may allow
     * multiple read sessions to be opened.
     *
     * @return the new read session
     */
    ReadSession openReadSession();
}
