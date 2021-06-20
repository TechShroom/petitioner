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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultUriConnector implements UriConnector {
    private final ExecutorService workExecutor;
    private final AsynchronousChannelGroup group;

    public DefaultUriConnector(ExecutorService workExecutor, AsynchronousChannelGroup group) {
        this.workExecutor = workExecutor;
        this.group = group;
    }

    @Override
    public CompletableFuture<AsynchronousByteChannel> connect(URI uri) {
        return FutureCompleter.newPromise(workExecutor, future -> {
            var port = uri.getPort();
            if (port == -1) {
                port = switch (uri.getScheme()) {
                    case "http" -> 80;
                    case "https" -> 443;
                    default -> throw new IllegalStateException("Invalid scheme: " + uri.getScheme());
                };
            }
            var addr = new InetSocketAddress(uri.getHost(), port);
            var channel = AsynchronousSocketChannel.open(group);
            var connectFtr = Completables.<Void>wrap((a, h) ->
                channel.connect(addr, a, h)
            );
            Completables.attachParent(connectFtr, future);
            connectFtr.thenAccept(__ -> future.complete(channel));
        });
    }

    @Override
    public void close() {
        this.group.shutdown();
        try {
            if (!this.group.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to terminate channel group");
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while awaiting termination");
        }
    }
}
