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
import com.techshroom.petitioner.core.HttpMethod;
import com.techshroom.petitioner.core.HttpRequest;
import com.techshroom.petitioner.core.HttpRequestBody;
import com.techshroom.petitioner.core.internal.Constants;
import com.techshroom.petitioner.core.io.ReadSession;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Http1RequestEncoder implements Encoder<HttpRequest> {
    // TODO sealed classes in 17
    private enum State {
        WRITE_REQUEST_LINE,
        WRITE_HEADERS,
        WRITE_BODY,
        CLOSED,
        ;
    }

    private record HeaderState(Iterator<Map.Entry<String, String>> entries) {
    }

    @Override
    public ReadSession encode(HttpRequest request) {
        return new ReadSession() {
            private final Lock lock = new ReentrantLock();
            private State state = State.WRITE_REQUEST_LINE;
            private Object attachment;

            @Override
            public CompletionStage<@NonNull ByteBuffer> readNextPacket() {
                lock.lock();
                try {
                    return switch (state) {
                        case WRITE_REQUEST_LINE -> {
                            // Write request line
                            var encoded = encodeRequestLine(request.method(), request.uri());
                            state = State.WRITE_HEADERS;
                            attachment = new HeaderState(buildHeaders(
                                request.uri(), request.headers(), request.body()
                            ));
                            yield CompletableFuture.completedFuture(encoded);
                        }
                        case WRITE_HEADERS -> {
                            var localState = (HeaderState) attachment;
                            if (!localState.entries.hasNext()) {
                                var body = request.body();
                                if (body != null) {
                                    state = State.WRITE_BODY;
                                    attachment = body.openReadSession();
                                } else {
                                    state = State.CLOSED;
                                    attachment = null;
                                }
                                yield CompletableFuture.completedFuture(ByteBuffer.wrap(new byte[]{'\r', '\n'}));
                            }
                            var encoded = encodeHeaderLine(localState.entries.next());
                            yield CompletableFuture.completedFuture(encoded);
                        }
                        case WRITE_BODY -> {
                            var localState = (ReadSession) attachment;
                            var packet = localState.readNextPacket();
                            // It doesn't matter if the packet is empty, we'd give CLOSED anyways
                            // We just need to know when we can adjust our state
                            yield packet.thenApply(bb -> {
                                if (bb.remaining() == 0) {
                                    state = State.CLOSED;
                                    attachment = null;
                                }
                                return bb;
                            });
                        }
                        case CLOSED -> CompletableFuture.completedFuture(
                            Constants.EMPTY_BYTE_BUFFER
                        );
                    };
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public void close() {
                lock.lock();
                try {
                    state = State.CLOSED;
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    private static ByteBuffer encodeRequestLine(HttpMethod method, URI uri) {
        var path = uri.getRawPath();
        if (path.isEmpty()) {
            path = "/";
        }
        return StandardCharsets.UTF_8.encode(
            method.name() + " " + path + " HTTP/1.1\r\n"
        );
    }

    private static ByteBuffer encodeHeaderLine(Map.Entry<String, String> next) {
        return StandardCharsets.UTF_8.encode(
            next.getKey() + ": " + next.getValue() + "\r\n"
        );
    }

    private Iterator<Map.Entry<String, String>> buildHeaders(URI uri, HttpHeaderMap headers,
                                                             @Nullable HttpRequestBody body) {
        var builder = headers.toBuilder();
        // It's mandatory that we include this
        builder.put("Host", uri.getHost());
        if (body != null) {
            body.contentLength().ifPresent(contentLength ->
                builder.put("Content-Length", String.valueOf(contentLength))
            );
            var contentTypeBuilder = new StringBuilder(body.contentType().mimeType());
            if (body.contentType().charset() != null) {
                contentTypeBuilder.append("; charset=").append(body.contentType().charset());
            }
            builder.put("Content-Type", contentTypeBuilder.toString());
        }
        return builder.build().iterator();
    }
}
