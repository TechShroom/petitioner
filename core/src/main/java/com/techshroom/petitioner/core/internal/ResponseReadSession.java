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

import com.techshroom.petitioner.core.io.ReadSession;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;

public class ResponseReadSession implements ReadSession {
    private final Semaphore semaphore = new Semaphore(1);
    private final ReadSession delegate;
    private final long contentLength;
    private long contentRead;
    private ByteBuffer leftoverFromHeader;
    private boolean closed;

    public ResponseReadSession(ReadSession delegate, long contentLength, ByteBuffer leftoverFromHeader) {
        this.delegate = delegate;
        this.contentLength = contentLength;
        this.leftoverFromHeader = leftoverFromHeader;
    }

    @Override
    public CompletionStage<@NonNull ByteBuffer> readNextPacket() {
        semaphore.acquireUninterruptibly();
        if (contentLength >= 0 && contentRead >= contentLength) {
            // Don't do a read, we've already got all the data we need for now
            // This allows persistent connections to function properly
            closed = true;
        }
        var future = closed
            ? CompletableFuture.completedFuture(Constants.EMPTY_BYTE_BUFFER)
            : commonCase();
        return future
            .whenComplete((buffer, ___) -> {
                try {
                    if (buffer != null) {
                        // Record content length
                        contentRead += buffer.remaining();
                    }
                } finally {
                    semaphore.release();
                }
            });
    }

    private CompletionStage<@NonNull ByteBuffer> commonCase() {
        if (leftoverFromHeader != null) {
            var nextPacket = leftoverFromHeader;
            leftoverFromHeader = null;
            return CompletableFuture.completedFuture(nextPacket);
        }
        return delegate.readNextPacket();
    }

    @Override
    public void close() throws IOException {
        semaphore.acquireUninterruptibly();
        try {
            closed = true;
            delegate.close();
        } finally {
            semaphore.release();
        }
    }
}
