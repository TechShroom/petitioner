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

import com.techshroom.petitioner.core.internal.select.Completables;
import com.techshroom.petitioner.core.io.ReadSession;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;

public class SocketChannelReadSession implements ReadSession {
    private final Semaphore semaphore = new Semaphore(1);
    private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(Constants.PACKET_SIZE);
    private final AsynchronousSocketChannel channel;

    public SocketChannelReadSession(AsynchronousSocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public CompletionStage<@NonNull ByteBuffer> readNextPacket() {
        semaphore.acquireUninterruptibly();
        var future = new CompletableFuture<ByteBuffer>();
        tryReadAndComplete(future);
        // Don't unlock for the next packet until we finish reading
        return future.whenComplete((__, ___) -> semaphore.release());
    }

    private void tryReadAndComplete(CompletableFuture<ByteBuffer> future) {
        var readFtr = Completables.<Integer>wrap((a, h) -> channel.read(readBuffer, a, h));
        Completables.attachParent(readFtr, future);
        readFtr.thenAccept(readCount -> {
            if (readCount == -1) {
                future.complete(Constants.EMPTY_BYTE_BUFFER);
                return;
            }
            if (readCount == 0) {
                // Doubt this will happen often
                readBuffer.clear();
                tryReadAndComplete(future);
                return;
            }
            readBuffer.flip();
            var nextPacket = ByteBuffer.allocate(readCount);
            nextPacket.put(readBuffer);
            nextPacket.flip();
            readBuffer.clear();
            future.complete(nextPacket.asReadOnlyBuffer());
        });
    }

    @Override
    public void close() throws IOException {
        // We only own the channel
        this.channel.close();
    }
}
