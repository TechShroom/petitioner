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

import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

public class Completables {
    public static void attachParent(CompletableFuture<?> child, CompletableFuture<?> parent) {
        child.whenComplete((v, ex) -> {
            if (ex != null) {
                parent.completeExceptionally(ex);
            }
        });
    }

    public static <T> CompletableFuture<T> wrap(
        AsyncCall<T, CompletableFuture<T>> bindable
    ) {
        var future = new CompletableFuture<T>();
        bindable.call(future, new CompletionHandler<>() {
            @Override
            public void completed(T result, CompletableFuture<T> attachment) {
                attachment.complete(result);
            }

            @Override
            public void failed(Throwable exc, CompletableFuture<T> attachment) {
                attachment.completeExceptionally(exc);
            }
        });
        return future;
    }
}
