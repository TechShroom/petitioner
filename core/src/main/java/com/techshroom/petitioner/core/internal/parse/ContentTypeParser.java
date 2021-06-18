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

package com.techshroom.petitioner.core.internal.parse;

import com.techshroom.petitioner.core.HttpContentType;
import com.techshroom.petitioner.core.internal.HttpEncoding;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ContentTypeParser {
    private final String source;
    private int index;

    public ContentTypeParser(String source) {
        this.source = source;
    }

    private String readableSource() {
        // potentially encode non-printables?
        return source;
    }

    public HttpContentType parse() {
        var type = readToken();
        if (!hasNextChar() || nextChar() != '/') {
            throw new IllegalStateException("Content-Type does not have sub-type: " + readableSource());
        }
        var subType = readToken();
        var parameters = new HashMap<String, String>();
        while (hasNextChar()) {
            eatLWS();
            if (nextChar() != ';') {
                throw new IllegalStateException("Unknown text following MIME-type: " + readableSource());
            }
            eatLWS();
            var attribute = readToken();
            if (!hasNextChar() || nextChar() != '=' || !hasNextChar()) {
                throw new IllegalStateException("Attribute does not have a matching value: " + readableSource());
            }
            String value;
            if (nextChar() == '"') {
                // quoted-string
                value = readQuotedString();
            } else {
                index--;
                // token
                value = readToken();
            }
            parameters.put(attribute, value);
        }
        var charset = parameters.get("charset");
        if (charset == null) {
            charset = switch (type) {
                case "text" -> StandardCharsets.ISO_8859_1.name();
                case "application" -> "json".equals(subType)
                    ? StandardCharsets.UTF_8.name()
                    : null;
                default -> null;
            };
        }
        return new HttpContentType(type + "/" + subType, charset);
    }

    private String readToken() {
        var builder = new StringBuilder();
        while (hasNextChar()) {
            var c = nextChar();
            if (!HttpEncoding.isValidNameChar(c)) {
                index--;
                break;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private String readQuotedString() {
        var builder = new StringBuilder();
        loop:
        while (true) {
            if (!hasNextChar()) {
                throw new IllegalArgumentException("Unfinished quoted string: " + readableSource());
            }
            var c = nextChar();
            switch (c) {
                case '\\' -> {
                    if (!hasNextChar()) {
                        throw new IllegalArgumentException("Unfinished quoted string: " + readableSource());
                    }
                    builder.append(nextChar());
                }
                case '"' -> {
                    break loop;
                }
                default -> builder.append(c);
            }
        }
        return builder.toString();
    }

    private void eatLWS() {
        if (!hasNextChar()) {
            return;
        }
        var c = nextChar();
        if (c == '\r') {
            if (!hasNextChar()) {
                // not CRLF, return
                index--;
                return;
            }
            c = nextChar();
            if (c != '\n') {
                // not CRLF, return
                index -= 2;
                return;
            }
        }
        index--;
        while (hasNextChar()) {
            c = nextChar();
            if (c != ' ' && c != '\t') {
                // not LWS, break
                index--;
                break;
            }
        }
    }

    private boolean hasNextChar() {
        return index < source.length();
    }

    private char nextChar() {
        var c = source.charAt(index);
        index++;
        return c;
    }
}
