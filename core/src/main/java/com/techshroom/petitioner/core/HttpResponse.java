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

public record HttpResponse(
    int statusCode,
    String statusMessage,
    HttpHeaderMap headers,
    @Nullable HttpResponseBody body
) {
    /**
     * {@return if the status code is between 100 and 200}
     */
    public boolean isInformational() {
        return 100 <= statusCode && statusCode < 200;
    }

    /**
     * {@return if the status code is between 200 and 300}
     */
    public boolean isSuccessful() {
        return 200 <= statusCode && statusCode < 300;
    }

    /**
     * {@return if the status code is between 300 and 400}
     */
    public boolean isRedirect() {
        return 300 <= statusCode && statusCode < 400;
    }

    /**
     * {@return if the status code represents an error} This means either
     * {@link #isClientError()} or {@link #isServerError()} returns {@code true}.
     */
    public boolean isError() {
        return isClientError() || isServerError();
    }

    /**
     * {@return if the status code is between 400 and 500}
     */
    public boolean isClientError() {
        return 400 <= statusCode && statusCode < 500;
    }

    /**
     * {@return if the status code is between 500 and 600}
     */
    public boolean isServerError() {
        return 500 <= statusCode && statusCode < 600;
    }
}
