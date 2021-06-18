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

package com.techshroom.petitioner.core.internal;

import com.techshroom.petitioner.core.HttpContentType;
import com.techshroom.petitioner.core.HttpRequestBody;
import com.techshroom.petitioner.core.io.ReadSession;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.ByteBuffer;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final record ByteBufferHttpRequestBody(
    ByteBuffer content,
    HttpContentType contentType
) implements HttpRequestBody {

    @Override
    public OptionalLong contentLength() {
        return OptionalLong.of(content.remaining());
    }

    @Override
    public HttpContentType contentType() {
        return contentType;
    }

    @Override
    public ReadSession openReadSession() {
        if (content.remaining() < Constants.PACKET_SIZE) {
            // We can just use the content itself
            return new ReadSession() {
                private final AtomicBoolean closed = new AtomicBoolean();

                @Override
                public CompletableFuture<@Nullable ByteBuffer> readNextPacket() {
                    // Try swapping in the closure, if we lose we already provided the packet
                    if (!closed.compareAndSet(false, true)) {
                        return CompletableFuture.completedFuture(Constants.EMPTY_BYTE_BUFFER);
                    }
                    return CompletableFuture.completedFuture(content.slice());
                }

                @Override
                public void close() {
                    closed.set(true);
                }
            };
        }
        return new ReadSession() {
            private final Lock lock = new ReentrantLock();
            private int index;

            @Override
            public CompletableFuture<@Nullable ByteBuffer> readNextPacket() {
                lock.lock();
                try {
                    int size = Math.min(Constants.PACKET_SIZE, content.remaining() - index);
                    if (size <= 0) {
                        return CompletableFuture.completedFuture(Constants.EMPTY_BYTE_BUFFER);
                    }
                    var buffer = content.slice(index, size);
                    index += size;
                    return CompletableFuture.completedFuture(buffer);
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public void close() {
                lock.lock();
                try {
                    // "close" by moving to the end of the buffer :)
                    index = content.remaining();
                } finally {
                    lock.unlock();
                }
            }
        };
    }
}
