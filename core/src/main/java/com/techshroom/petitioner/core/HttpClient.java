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

package com.techshroom.petitioner.core;

import com.techshroom.petitioner.core.internal.Constants;
import com.techshroom.petitioner.core.internal.codec.HttpCodec;
import com.techshroom.petitioner.core.socket.AsyncIoHttpClient;
import com.techshroom.petitioner.core.socket.DefaultUriConnector;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

/**
 * Represents an HTTP client.
 */
public interface HttpClient extends Closeable {

    final class Builder {
        private ExecutorService executor = Constants.DEFAULT_WORK_EXECUTOR;
        private HttpVersion version = HttpVersion.VERSION_1_1;

        private Builder() {
        }

        public Builder executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Builder version(HttpVersion version) {
            this.version = version;
            return this;
        }

        public HttpClient build() {
            AsynchronousChannelGroup group;
            try {
                group = AsynchronousChannelGroup.withThreadPool(executor);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new AsyncIoHttpClient(
                executor,
                new DefaultUriConnector(executor, group),
                HttpCodec.forVersion(version)
            );
        }
    }

    static HttpClient create() {
        return builder().build();
    }

    static Builder builder() {
        return new Builder();
    }

    /**
     * Execute a request asynchronously.
     *
     * <p>
     * If an error occurs while sending the request or receiving the response,
     * the returned completion stage will fail with that error.
     * </p>
     *
     * @param request the request to execute
     * @return the completion stage that will result in a response or error
     */
    CompletionStage<HttpResponse> executeAsync(HttpRequest request);
}
