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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReadSessionInputStream extends InputStream {
    private final ReadSession readSession;
    private final Lock lock = new ReentrantLock();
    private ByteBuffer currentPacket;
    private boolean closed;

    public ReadSessionInputStream(ReadSession readSession) {
        this.readSession = readSession;
    }

    /*
     * Invariant: if this method returns normally, EITHER:
     * 1. currentPacket is non-null and has remaining bytes
     * 2. closed is set to true
     */
    private void fillBufferIfNeeded() throws IOException {
        if (closed) {
            return;
        }
        if (currentPacket == null || !currentPacket.hasRemaining()) {
            currentPacket = readSession.readNextPacket().toCompletableFuture().join();
            if (!currentPacket.hasRemaining()) {
                closed = true;
            }
        }
    }

    @Override
    public int read() throws IOException {
        lock.lock();
        try {
            fillBufferIfNeeded();
            if (closed) {
                return -1;
            }
            return currentPacket.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            fillBufferIfNeeded();
            if (closed) {
                return -1;
            }
            int realLen = Math.min(currentPacket.remaining(), len);
            currentPacket.get(b, off, realLen);
            return realLen;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            closed = true;
            currentPacket = null;
            readSession.close();
        } finally {
            lock.unlock();
        }
    }
}
