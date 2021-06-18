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

public record HttpRequest(
    HttpMethod method,
    URI uri,
    HttpHeaderMap headers,
    @Nullable HttpRequestBody body
) {
    public HttpRequest {
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
}
