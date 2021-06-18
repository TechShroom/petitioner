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

import com.techshroom.petitioner.core.HttpClient;
import com.techshroom.petitioner.core.HttpHeaderMap;
import com.techshroom.petitioner.core.HttpMethod;
import com.techshroom.petitioner.core.HttpRequest;
import com.techshroom.petitioner.core.HttpVersion;
import com.techshroom.petitioner.core.internal.Constants;
import com.techshroom.petitioner.core.internal.codec.HttpCodec;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class AsyncIoHttpClientTest {
    private final HttpClient client = new AsyncIoHttpClient(
        Constants.DEFAULT_WORK_EXECUTOR,
        HttpCodec.forVersion(HttpVersion.VERSION_1_1)
    );

    @Test
    void simpleGet() throws Exception {
        var response = client.executeAsync(new HttpRequest(
            HttpMethod.GET,
            URI.create("http://httpbin.org/get"),
            HttpHeaderMap.empty(),
            null
        )).toCompletableFuture().get(5, TimeUnit.SECONDS);
        System.err.println(response);
        if (response.body() != null) {
            System.err.println(response.body().string());
        }
    }
}
