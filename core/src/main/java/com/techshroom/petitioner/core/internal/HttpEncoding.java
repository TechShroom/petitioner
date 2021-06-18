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

import java.util.BitSet;

public class HttpEncoding {

    private static final BitSet VALID_NAME_CHARS = new BitSet();
    private static final BitSet VALID_VALUE_CHARS = new BitSet();

    static {
        // any CHAR (0-127) except CTLs
        VALID_NAME_CHARS.set(0x21, 128);
        // (DEL is a CTL)
        VALID_NAME_CHARS.set(0x7F, false);
        // or separators
        for (char c : "()<>@,;:\\\"/[]?={} \t".toCharArray()) {
            VALID_NAME_CHARS.set(c, false);
        }

        // any OCTET except CTLs
        VALID_VALUE_CHARS.set(0x21, 0x100);
        // (DEL is a CTL)
        VALID_VALUE_CHARS.set(0x7F, false);
        // but including LWS (space and tab)
        VALID_VALUE_CHARS.set(' ');
        VALID_VALUE_CHARS.set('\t');
    }

    public static boolean isValidName(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!isValidNameChar(c)) {
                return false;
            }
        }
        return !token.isEmpty();
    }

    public static boolean isValidNameChar(char c) {
        return c <= 255 && VALID_NAME_CHARS.get(c);
    }

    public static boolean isValidValue(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c > 255 || !VALID_VALUE_CHARS.get(c)) {
                return false;
            }
        }
        return true;
    }
}
