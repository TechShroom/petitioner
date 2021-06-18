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

package com.techshroom.petitioner.core.internal;

import com.techshroom.petitioner.core.HttpContentType;
import com.techshroom.petitioner.core.HttpResponseBody;
import com.techshroom.petitioner.core.io.ReadSession;

import java.io.IOException;
import java.util.OptionalLong;

public record ReadSessionHttpResponseBody(
    OptionalLong contentLength,
    HttpContentType contentType,
    ReadSession readSession
) implements HttpResponseBody {
    @Override
    public void close() throws IOException {
        readSession.close();
    }
}
