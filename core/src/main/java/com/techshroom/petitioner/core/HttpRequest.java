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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record HttpRequest(
    HttpMethod method,
    URI uri,
    HttpHeaderMap headers,
    @Nullable HttpRequestBody body
) {
    public HttpRequest {
        if (method == null) {
            throw new IllegalArgumentException("method must be provided");
        }
        if (uri == null) {
            throw new IllegalArgumentException("uri must be provided");
        }
        if (headers == null) {
            throw new IllegalArgumentException("headers must be provided");
        }
        if (!method.supportsBody() && body != null) {
            throw new IllegalArgumentException(
                "Cannot provide a request body for a " + method.name() + " request"
            );
        }
        if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
            throw new IllegalArgumentException(
                "URI must be either http or https"
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static HttpRequest get(String uri) {
        return builder().get(uri).build();
    }

    public static HttpRequest head(String uri) {
        return builder().head(uri).build();
    }

    public static HttpRequest post(String uri, @Nullable HttpRequestBody body) {
        return builder().post(uri, body).build();
    }

    public static HttpRequest put(String uri, @Nullable HttpRequestBody body) {
        return builder().put(uri, body).build();
    }

    public static HttpRequest delete(String uri) {
        return builder().delete(uri).build();
    }

    public static HttpRequest connect(String uri) {
        return builder().connect(uri).build();
    }

    public static HttpRequest options(String uri) {
        return builder().options(uri).build();
    }

    public static HttpRequest trace(String uri) {
        return builder().trace(uri).build();
    }

    public static HttpRequest patch(String uri, @Nullable HttpRequestBody body) {
        return builder().patch(uri, body).build();
    }

    public static final class Builder {
        private HttpMethod method;
        private URI uri;
        private final HttpHeaderMap.Builder headersBuilder;
        private HttpRequestBody body;

        private Builder() {
            this.headersBuilder = HttpHeaderMap.builder();
        }

        private Builder(HttpRequest base) {
            this.method = base.method;
            this.uri = base.uri;
            this.headersBuilder = base.headers.toBuilder();
            this.body = base.body;
        }

        public Builder get(String uri) {
            return method(HttpMethod.GET).uri(uri);
        }

        public Builder head(String uri) {
            return method(HttpMethod.HEAD).uri(uri);
        }

        public Builder post(String uri, @Nullable HttpRequestBody body) {
            return method(HttpMethod.POST).uri(uri).body(body);
        }

        public Builder put(String uri, @Nullable HttpRequestBody body) {
            return method(HttpMethod.PUT).uri(uri).body(body);
        }

        public Builder delete(String uri) {
            return method(HttpMethod.DELETE).uri(uri);
        }

        public Builder connect(String uri) {
            return method(HttpMethod.CONNECT).uri(uri);
        }

        public Builder options(String uri) {
            return method(HttpMethod.OPTIONS).uri(uri);
        }

        public Builder trace(String uri) {
            return method(HttpMethod.TRACE).uri(uri);
        }

        public Builder patch(String uri, @Nullable HttpRequestBody body) {
            return method(HttpMethod.PATCH).uri(uri).body(body);
        }

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder uri(String uri) {
            return uri(URI.create(uri));
        }

        public Builder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        public Builder putHeader(String name, String value) {
            this.headersBuilder.put(name, value);
            return this;
        }

        public Builder putHeader(String name, List<String> value) {
            this.headersBuilder.put(name, value);
            return this;
        }

        public Builder addHeader(String name, String value) {
            this.headersBuilder.add(name, value);
            return this;
        }

        public Builder putAllHeaders(Map<String, List<String>> entries) {
            this.headersBuilder.putAll(entries);
            return this;
        }

        public Builder putAllHeaders(HttpHeaderMap headerMap) {
            this.headersBuilder.putAll(headerMap);
            return this;
        }

        public Builder body(@Nullable HttpRequestBody body) {
            this.body = body;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(
                method,
                uri,
                headersBuilder.build(),
                body
            );
        }
    }

    public Builder toBuilder() {
        return new Builder(this);
    }
}
