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

package com.techshroom.petitioner.core.internal.select;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Kinda like the callback of {@code new Promise} in JavaScript. Exceptions will fail,
 * but the call doesn't provide a value back immediately.
 */
public interface FutureCompleter<T> {
    static <T> CompletableFuture<T> newPromise(Executor executor, FutureCompleter<T> completer) {
        var future = new CompletableFuture<T>();
        executor.execute(() -> completeFuture(future, completer));
        return future;
    }

    static <T> void completeFuture(CompletableFuture<T> future, FutureCompleter<T> completer) {
        try {
            completer.complete(future);
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
    }

    void complete(CompletableFuture<T> future) throws Exception;
}
