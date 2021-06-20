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

import com.techshroom.petitioner.core.internal.select.Completables;
import com.techshroom.petitioner.core.internal.select.FutureCompleter;
import org.checkerframework.checker.nullness.qual.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ReadPendingException;
import java.nio.channels.WritePendingException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestUriConnector implements UriConnector {
    private final ExecutorService executor;
    private final Map<URI, TestAsyncByteChannel> openChannels = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public TestUriConnector(ExecutorService executor) {
        this.executor = executor;
    }

    public @NonNull TestAsyncByteChannel createChannel(URI uri) {
        var createdNew = new AtomicBoolean();
        var channel = openChannels.computeIfAbsent(uri, k -> {
            createdNew.setPlain(true);
            return new TestAsyncByteChannel(executor, () -> openChannels.remove(k));
        });
        if (!createdNew.getPlain()) {
            // the URI is the unique key for testing
            // however, since URIs can have fragments, that can be used to open two to the same "path"
            // as the HTTP client will just ignore the fragments
            throw new AssertionError("channel is already open for URI: " + uri);
        }
        return channel;
    }

    @Override
    public CompletableFuture<AsynchronousByteChannel> connect(URI uri) {
        return FutureCompleter.newPromise(ForkJoinPool.commonPool(), future -> {
            if (closed) {
                throw new IllegalStateException("closed");
            }
            var channel = openChannels.get(uri);
            if (channel == null) {
                throw new IllegalStateException("Channel wasn't created by test for " + uri);
            }
            future.complete(channel);
        });
    }

    @Override
    public void close() {
        this.closed = true;
        while (!openChannels.isEmpty()) {
            for (var iter = openChannels.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<URI, TestAsyncByteChannel> next;
                try {
                    next = iter.next();
                } catch (NoSuchElementException e) {
                    // Loop again
                    break;
                }
                next.getValue().closed.set(true);
                iter.remove();
            }
        }
    }

    public static final class TestAsyncByteChannel implements AsynchronousByteChannel {

        private final ExecutorService executor;
        private final Runnable closeNotification;
        private final AtomicBoolean closed = new AtomicBoolean();
        private final BlockingDeque<ByteBuffer> reads = new LinkedBlockingDeque<>();
        private final Sinks.Many<ByteBuffer> writes = Sinks.many().unicast().onBackpressureBuffer();
        private ByteBuffer leftoverRead;
        private final AtomicBoolean reading = new AtomicBoolean();
        private final AtomicBoolean writing = new AtomicBoolean();

        private TestAsyncByteChannel(ExecutorService executor, Runnable closeNotification) {
            this.executor = executor;
            this.closeNotification = closeNotification;
        }

        public void addReadableBytes(ByteBuffer buffer) {
            reads.add(buffer);
        }

        public Flux<ByteBuffer> getWrittenBytes() {
            return writes.asFlux();
        }

        @Override
        public <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler) {
            if (!reading.compareAndSet(false, true)) {
                throw new ReadPendingException();
            }
            executor.submit(() -> {
                try {
                    var message = leftoverRead != null
                        ? leftoverRead
                        : reads.takeFirst().slice();
                    leftoverRead = null;
                    if (closed.get()) {
                        throw new AsynchronousCloseException();
                    }
                    dst.put(message);
                    if (message.hasRemaining()) {
                        leftoverRead = message;
                    }
                    reading.set(false);
                    handler.completed(message.position(), attachment);
                } catch (Exception e) {
                    reading.set(false);
                    handler.failed(e, attachment);
                }
            });
        }

        @Override
        public Future<Integer> read(ByteBuffer dst) {
            return Completables.wrap((a, h) -> read(dst, a, h));
        }

        @Override
        public <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler) {
            if (!writing.compareAndSet(false, true)) {
                throw new WritePendingException();
            }
            executor.submit(() -> {
                try {
                    if (closed.get()) {
                        throw new AsynchronousCloseException();
                    }
                    var clone = ByteBuffer.allocate(src.remaining());
                    clone.put(src);
                    clone.flip();
                    var emitResult = writes.tryEmitNext(clone);
                    if (emitResult.isFailure()) {
                        throw new IOException("Failed to emit write: " + emitResult);
                    }
                    writing.set(false);
                    handler.completed(clone.capacity(), attachment);
                } catch (Exception e) {
                    writing.set(false);
                    handler.failed(e, attachment);
                }
            });
        }

        @Override
        public Future<Integer> write(ByteBuffer src) {
            return Completables.wrap((a, h) -> write(src, a, h));
        }

        @Override
        public boolean isOpen() {
            return !closed.get();
        }

        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true)) {
                var emitResult = writes.tryEmitComplete();
                if (emitResult.isFailure()) {
                    this.closeNotification.run();
                    throw new IllegalStateException("Failed to complete writes: " + emitResult);
                }
                this.closeNotification.run();
            }
        }
    }
}
