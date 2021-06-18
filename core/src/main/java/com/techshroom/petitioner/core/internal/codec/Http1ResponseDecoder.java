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

package com.techshroom.petitioner.core.internal.codec;

import com.techshroom.petitioner.core.HttpHeaderMap;
import com.techshroom.petitioner.core.internal.HttpEncoding;
import com.techshroom.petitioner.core.internal.PartialHttpResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

public class Http1ResponseDecoder implements Decoder<@NonNull PartialHttpResponse> {
    private static final Pattern STATUS_CODE = Pattern.compile("\\d\\d\\d");

    private final Lock lock = new ReentrantLock();
    private int statusCode;
    private String statusMessage;
    private final HttpHeaderMap.Builder headers = HttpHeaderMap.builder();

    @Override
    public @Nullable PartialHttpResponse tryDecode(ByteBuffer buffer) {
        lock.lock();
        try {
            String nextLine;
            while ((nextLine = readNextLine(buffer)) != null) {
                if (statusMessage == null) {
                    decodeStatusLine(nextLine);
                } else {
                    if (nextLine.isEmpty()) {
                        // end of headers encountered
                        return new PartialHttpResponse(
                            statusCode,
                            statusMessage,
                            headers.build()
                        );
                    }
                    decodeHeaderLine(nextLine);
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    private void decodeStatusLine(String line) {
        String[] parts = line.split(" ", 3);
        if (parts.length < 3) {
            throw new IllegalStateException("Invalid status line: " + line);
        }
        if (!"HTTP/1.1".equals(parts[0]) && !"HTTP/1.0".equals(parts[0])) {
            throw new IllegalStateException("Not using HTTP/1.1: " + line);
        }
        if (!STATUS_CODE.matcher(parts[1]).matches()) {
            throw new IllegalStateException("Status code is not 3DIGIT: " + line);
        }
        statusCode = Integer.parseInt(parts[1]);
        statusMessage = parts[2];
    }

    private void decodeHeaderLine(String line) {
        String[] parts = line.split(":", 2);
        if (parts.length < 2) {
            throw new IllegalStateException("Invalid header line: " + line);
        }
        var name = parts[0].trim();
        if (!HttpEncoding.isValidName(name)) {
            throw new IllegalStateException("Invalid name: " + line);
        }
        var value = parts[1].trim();
        if (!HttpEncoding.isValidValue(value)) {
            throw new IllegalStateException("Invalid value: " + line);
        }
        headers.add(name, value);
    }

    private String readNextLine(ByteBuffer buffer) {
        buffer.mark();
        var builder = new StringBuilder();
        while (buffer.hasRemaining()) {
            var c = (char) buffer.get();
            if (c == '\r' && buffer.hasRemaining()) {
                if (buffer.get() == '\n') {
                    // CRLF detected, line finished
                    return builder.toString();
                }
                // no LF, reset back a char
                buffer.position(buffer.position() - 1);
            }
            builder.append(c);
        }
        buffer.reset();
        return null;
    }
}
