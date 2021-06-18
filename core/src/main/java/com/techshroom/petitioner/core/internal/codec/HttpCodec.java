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

import com.techshroom.petitioner.core.HttpRequest;
import com.techshroom.petitioner.core.HttpVersion;
import com.techshroom.petitioner.core.internal.PartialHttpResponse;

import java.util.function.Supplier;

public record HttpCodec(
    Supplier<Encoder<HttpRequest>> requestEncoder,
    Supplier<Decoder<PartialHttpResponse>> responseDecoder
) {
    public static HttpCodec forVersion(HttpVersion version) {
        return switch (version) {
            case VERSION_1_1 -> new HttpCodec(
                Http1RequestEncoder::new, Http1ResponseDecoder::new
            );
            case VERSION_2, VERSION_3 -> throw new UnsupportedOperationException(
                version + " is not implemented yet!"
            );
        };
    }
}
