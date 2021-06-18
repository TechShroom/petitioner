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

package com.techshroom.petitioner.core.io;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Reads a single stream. This allows the request body to be repeatedly read,
 * by tracking read progress separately.
 *
 * <p>
 * Read sessions must be thread-safe, but they do not need to be concerned with
 * throughput and may simply use a {@link ReentrantLock} to guard public
 * methods.
 * </p>
 */
public interface ReadSession extends Closeable {
    /**
     * {@return the next packet} If there's no next packet, return an empty packet.
     */
    CompletionStage<@NonNull ByteBuffer> readNextPacket();
}
