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

import com.techshroom.petitioner.core.internal.ReadSessionInputStream;
import com.techshroom.petitioner.core.io.ReadSession;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Represents the body of an HTTP response.
 *
 * <p>
 * Should be closed when you no longer need it, in order to unpin the underlying connection.
 * </p>
 */
public interface HttpResponseBody extends Closeable {
    /**
     * {@return the length of content in the body} May by {@link OptionalLong#empty()} if there is no
     * known length.
     */
    OptionalLong contentLength();

    /**
     * {@return the type of content in the body} Used to auto-decode the input stream to characters if needed.
     */
    HttpContentType contentType();

    /**
     * Read this body using a {@link ReadSession}. Closing the read session also closes the body.
     *
     * <p>
     * Note that unlike a request body, a response body will only ever produce one read session.
     * </p>
     *
     * @return the read session to read from
     */
    ReadSession readSession();

    /**
     * Read this body as an {@link InputStream}. Closing this input stream also closes the body.
     *
     * @return the input stream to read from
     */
    default InputStream inputStream() {
        return new ReadSessionInputStream(readSession());
    }

    /**
     * Read this body as a {@link Reader}. Closing this reader also closes the body.
     *
     * @return the reader to read from
     * @implNote The content is decoded from {@link #inputStream()} using the charset of {@link #contentType()},
     * or UTF-8 if none is given
     */
    default Reader reader() {
        var charset = Objects.requireNonNullElse(contentType().resolveCharset(), StandardCharsets.UTF_8);
        return new InputStreamReader(inputStream(), charset);
    }

    /**
     * Read this body as a byte array. Automatically closes the body as well.
     *
     * @return the bytes read from the {@link #inputStream()}
     * @throws IOException re-thrown from reading
     */
    default byte[] bytes() throws IOException {
        try (var stream = inputStream()) {
            return stream.readAllBytes();
        }
    }

    /**
     * Read this body as a {@link String}. Automatically closes the body as well.
     *
     * @return the string read from the {@link #reader()}
     * @throws IOException re-thrown from reading
     */
    default String string() throws IOException {
        try (var reader = reader()) {
            var capture = new StringWriter((int) Math.min(Integer.MAX_VALUE, contentLength().orElse(16)));
            reader.transferTo(capture);
            return capture.toString();
        }
    }
}
