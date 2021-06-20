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

package com.techshroom.petitioner.core.socket;

import com.techshroom.petitioner.core.HttpClient;
import com.techshroom.petitioner.core.HttpContentType;
import com.techshroom.petitioner.core.HttpHeaderMap;
import com.techshroom.petitioner.core.HttpRequest;
import com.techshroom.petitioner.core.HttpResponse;
import com.techshroom.petitioner.core.internal.PartialHttpResponse;
import com.techshroom.petitioner.core.internal.ReadSessionHttpResponseBody;
import com.techshroom.petitioner.core.internal.ResponseReadSession;
import com.techshroom.petitioner.core.internal.ByteChannelReadSession;
import com.techshroom.petitioner.core.internal.codec.Decoder;
import com.techshroom.petitioner.core.internal.codec.HttpCodec;
import com.techshroom.petitioner.core.internal.parse.ContentTypeParser;
import com.techshroom.petitioner.core.internal.select.Completables;
import com.techshroom.petitioner.core.internal.select.FutureCompleter;
import com.techshroom.petitioner.core.io.ReadSession;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An HTTP client using the new NIO2 APIs.
 */
public final class AsyncIoHttpClient implements HttpClient {
    private final ExecutorService workExecutor;
    private final UriConnector uriConnector;
    private final HttpCodec codec;

    public AsyncIoHttpClient(ExecutorService workExecutor, UriConnector uriConnector, HttpCodec codec) {
        this.workExecutor = workExecutor;
        this.uriConnector = uriConnector;
        this.codec = codec;
    }

    @Override
    public CompletionStage<HttpResponse> executeAsync(HttpRequest request) {
        var connectedFuture = initiateConnection(request);
        var sentFuture = connectedFuture.thenCompose(channel -> sendRequest(channel, request));
        return sentFuture.thenCompose(this::readResponseHeader);
    }

    private CompletableFuture<AsynchronousByteChannel> initiateConnection(HttpRequest request) {
        return uriConnector.connect(request.uri());
    }

    private CompletableFuture<AsynchronousByteChannel> sendRequest(AsynchronousByteChannel channel, HttpRequest request) {
        return FutureCompleter.newPromise(workExecutor, new FutureCompleter<>() {
            private final ReadSession readSession = codec.requestEncoder().get().encode(request);

            @Override
            public void complete(CompletableFuture<AsynchronousByteChannel> future) {
                readSession.readNextPacket()
                    .thenCompose(buffer -> {
                        if (!buffer.hasRemaining()) {
                            return CompletableFuture.completedFuture(false);
                        }
                        return writeFully(channel, buffer).thenApply(__ -> true);
                    })
                    .whenComplete((shouldContinue, ex) -> {
                        if (ex != null) {
                            future.completeExceptionally(ex);
                            return;
                        }
                        if (shouldContinue) {
                            // Continue the write loop
                            this.complete(future);
                        } else {
                            // Complete the top future
                            future.complete(channel);
                        }
                    });
            }
        });
    }

    private CompletableFuture<AsynchronousByteChannel> writeFully(AsynchronousByteChannel channel, ByteBuffer buffer) {
        return FutureCompleter.newPromise(workExecutor, new FutureCompleter<>() {
            @Override
            public void complete(CompletableFuture<AsynchronousByteChannel> fut) {
                var writeFuture = Completables.<Integer>wrap((a, h) -> channel.write(buffer, a, h));
                Completables.attachParent(writeFuture, fut);
                writeFuture.thenAccept(__ -> {
                    if (buffer.hasRemaining()) {
                        // It's not complete, call it again!
                        this.complete(fut);
                    } else {
                        // We've written everything :)
                        fut.complete(channel);
                    }
                });
            }
        });
    }

    private CompletionStage<HttpResponse> readResponseHeader(AsynchronousByteChannel channel) {
        return FutureCompleter.newPromise(workExecutor, new FutureCompleter<>() {
            private final ReadSession readSession = new ByteChannelReadSession(channel);
            private final Decoder<@NonNull PartialHttpResponse> responseDecoder = codec.responseDecoder().get();
            private ByteBuffer previousUnread;

            @Override
            public void complete(CompletableFuture<HttpResponse> future) {
                readSession.readNextPacket()
                    .thenAccept(buffer -> {
                        int previousRemaining;
                        ByteBuffer decodeBuf;
                        if (previousUnread == null) {
                            // Nothing special
                            previousRemaining = 0;
                            decodeBuf = buffer;
                        } else {
                            previousRemaining = previousUnread.remaining();
                            // Prepend the unread buffer into a new buffer
                            decodeBuf = ByteBuffer.allocateDirect(previousUnread.remaining() + buffer.remaining());
                            decodeBuf.put(previousUnread);
                            decodeBuf.put(buffer);
                            decodeBuf.flip();

                            previousUnread = null;
                        }
                        var partial = responseDecoder.tryDecode(decodeBuf);
                        if (partial == null) {
                            if (decodeBuf.hasRemaining()) {
                                previousUnread = decodeBuf;
                            }
                            if (!buffer.hasRemaining()) {
                                // We've reached EOF, but no decoded response
                                // If the decoder consumed all content, it's truly EOF
                                // Otherwise, it could be that the decoder wants to be called again
                                // But it has to consume some of the buffer each time.
                                if (!decodeBuf.hasRemaining() || decodeBuf.remaining() == previousRemaining) {
                                    throw new IllegalStateException("EOF reached prematurely");
                                }
                            }
                            // we need to try again
                            complete(future);
                            return;
                        }
                        var contentLength = decodeContentLength(partial.headers());
                        future.complete(new HttpResponse(
                            partial.statusCode(),
                            partial.statusMessage(),
                            partial.headers(),
                            new ReadSessionHttpResponseBody(
                                contentLength,
                                decodeContentType(partial.headers()),
                                new ResponseReadSession(
                                    readSession,
                                    contentLength.orElse(-1),
                                    decodeBuf.hasRemaining() ? decodeBuf : null
                                )
                            )
                        ));
                    })
                    .whenComplete((v, ex) -> future.completeExceptionally(ex));
            }
        });
    }

    private OptionalLong decodeContentLength(HttpHeaderMap headers) {
        String value = headers.value("Content-Length");
        if (value == null) {
            return OptionalLong.empty();
        }
        long parsed;
        try {
            parsed = Long.parseLong(value);
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
        if (parsed < 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(parsed);
    }

    private HttpContentType decodeContentType(HttpHeaderMap headers) {
        String value = headers.value("Content-Type");
        if (value == null) {
            // No guessing right now
            return new HttpContentType("application/octet-stream", null);
        }

        return new ContentTypeParser(value).parse();
    }

    @Override
    public void close() throws IOException {
        this.uriConnector.close();
    }
}
