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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.ByteBuffer;

public interface Decoder<T extends @NonNull Object> {
    /**
     * Try decoding the object, consuming the buffer in the process.
     *
     * <p>
     * If the object is decoded, return it. If not, return {@code null}. If it's not decoded,
     * data leftover in the buffer will be provided next call, at the start of the buffer.
     * If the object is decoded, then the leftover data will be taken back as if it was never
     * read.
     * </p>
     *
     * <p>
     * This method should be thread-safe, but it will not be called from multiple threads at once.
     * </p>
     */
    @Nullable T tryDecode(ByteBuffer buffer);
}
