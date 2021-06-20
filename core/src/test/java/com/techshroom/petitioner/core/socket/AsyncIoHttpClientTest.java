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
import com.techshroom.petitioner.core.HttpContentType;
import com.techshroom.petitioner.core.HttpRequest;
import com.techshroom.petitioner.core.HttpVersion;
import com.techshroom.petitioner.core.internal.Constants;
import com.techshroom.petitioner.core.internal.codec.HttpCodec;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

public class AsyncIoHttpClientTest {
    private static final String TEST_HOST = "petitioner.octyl.net";
    private final TestUriConnector connector = new TestUriConnector(Constants.DEFAULT_WORK_EXECUTOR);
    private final HttpClient client = new AsyncIoHttpClient(
        Constants.DEFAULT_WORK_EXECUTOR,
        connector,
        HttpCodec.forVersion(HttpVersion.VERSION_1_1)
    );

    @Test
    void simpleGet() throws Exception {
        var testText = "This is a test that stuff can be read. UTF-8 compatible: 👍";
        var testTextBytes = testText.getBytes(StandardCharsets.UTF_8);

        var request = HttpRequest.get("http://" + TEST_HOST + "/get");

        TestUriConnector.TestAsyncByteChannel channel = connector.createChannel(request.uri());

        // Response
        channel.addReadableBytes(StandardCharsets.UTF_8.encode("""
            HTTP/1.0 200 OK\r
            Content-length: %s\r
            Content-type: text/plain; charset=utf-8\r
            \r
            %s""".formatted(testTextBytes.length, testText)));

        var response = client.executeAsync(request)
            .toCompletableFuture().get(1, TimeUnit.MINUTES);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.statusMessage()).isEqualTo("OK");
        assertThat(response.body()).isNotNull();
        assertThat(response.body().contentLength()).isEqualTo(OptionalLong.of(testTextBytes.length));
        assertThat(response.body().contentType()).isEqualTo(new HttpContentType("text/plain", "utf-8"));
        assertThat(response.body().string()).isEqualTo(testText);

        // Get and check response
        var content = channel.getWrittenBytes()
            .map(b -> StandardCharsets.UTF_8.decode(b).toString())
            .collect(Collectors.joining(""))
            .block(Duration.ofMinutes(1));
        assertThat(content).isEqualTo("""
            GET /get HTTP/1.1\r
            Host: %s\r
            \r
            """.formatted(TEST_HOST));
    }
}
