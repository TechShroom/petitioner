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

/**
 * A listing of common HTTP methods.
 */
public enum HttpMethod {
    // Technically, GET, DELETE, CONNECT supports a body -- but in general it shouldn't be done
    // Open an issue if you need this for some reason.
    GET(false),
    HEAD(false),
    POST(true),
    PUT(true),
    DELETE(false),
    CONNECT(false),
    OPTIONS(false),
    TRACE(false),
    PATCH(true),
    ;

    private final boolean supportsBody;

    HttpMethod(boolean supportsBody) {
        this.supportsBody = supportsBody;
    }

    public boolean supportsBody() {
        return supportsBody;
    }
}
