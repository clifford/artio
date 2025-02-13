/*
 * Copyright 2015-2017 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.artio.dictionary.generation;

import org.agrona.MutableDirectBuffer;

import static java.nio.charset.StandardCharsets.US_ASCII;

public final class CodecUtil
{
    private CodecUtil()
    {
    }

    public static final byte[] BODY_LENGTH = "9=0000\001".getBytes(US_ASCII);

    public static final int MISSING_INT = Integer.MIN_VALUE;
    public static final char MISSING_CHAR = '\001';
    public static final long MISSING_LONG = Long.MIN_VALUE;

    public static final char ENUM_MISSING_CHAR = MISSING_CHAR;
    public static final int ENUM_MISSING_INT = MISSING_INT;
    public static final String ENUM_MISSING_STRING = Character.toString(ENUM_MISSING_CHAR);

    public static final char ENUM_UNKNOWN_CHAR = '\002';
    public static final int ENUM_UNKNOWN_INT = Integer.MAX_VALUE;
    public static final String ENUM_UNKNOWN_STRING = Character.toString(ENUM_UNKNOWN_CHAR);

    // NB: only valid for ASCII bytes.
    @Deprecated // Will be removed in a future version
    public static byte[] toBytes(final CharSequence value, final byte[] oldBuffer)
    {
        final int length = value.length();
        final byte[] buffer = (oldBuffer.length < length) ? new byte[length] : oldBuffer;
        for (int i = 0; i < length; i++)
        {
            buffer[i] = (byte)value.charAt(i);
        }

        return buffer;
    }

    // NB: only valid for ASCII bytes.
    @Deprecated // Will be removed in a future version
    public static byte[] toBytes(final char[] value, final byte[] oldBuffer, final int length)
    {
        return toBytes(value, oldBuffer, 0, length);
    }

    // NB: only valid for ASCII bytes.
    @Deprecated // Will be removed in a future version
    public static byte[] toBytes(final char[] value, final byte[] oldBuffer, final int offset, final int length)
    {
        final byte[] buffer = (oldBuffer.length < length) ? new byte[length] : oldBuffer;
        for (int i = 0; i < length; i++)
        {
            buffer[i] = (byte)value[i + offset];
        }
        return buffer;
    }

    // NB: only valid for ASCII bytes.
    public static byte[] toBytes(final char[] value, final int length)
    {
        final byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++)
        {
            buffer[i] = (byte)value[i];
        }

        return buffer;
    }

    // NB: only valid for ASCII bytes.
    public static void toBytes(final CharSequence value, final MutableDirectBuffer buffer)
    {
        final int length = value.length();
        if (buffer.capacity() < length)
        {
            buffer.wrap(new byte[length]);
        }

        for (int i = 0; i < length; i++)
        {
            buffer.putByte(i, (byte)value.charAt(i));
        }
    }

    // NB: only valid for ASCII bytes.
    public static void toBytes(final char[] value, final MutableDirectBuffer oldBuffer, final int length)
    {
        toBytes(value, oldBuffer, 0, length);
    }

    // NB: only valid for ASCII bytes.
    public static void toBytes(
        final char[] value, final MutableDirectBuffer buffer, final int offset, final int length)
    {
        if (buffer.capacity() < length)
        {
            buffer.wrap(new byte[length]);
        }

        for (int i = 0; i < length; i++)
        {
            buffer.putByte(i, (byte)value[i + offset]);
        }
    }

    public static boolean equals(
        final char[] value,
        final char[] expected,
        final int offset,
        final int expectedOffset,
        final int length)
    {
        if (value.length < length || expected.length < length)
        {
            return false;
        }

        for (int i = 0; i < length; i++)
        {
            if (value[i + offset] != expected[i + expectedOffset])
            {
                return false;
            }
        }

        return true;
    }

    public static boolean equals(final char[] value, final char[] expected, final int length)
    {
        return equals(value, expected, 0, 0, length);
    }

    public static int hashCode(final char[] value, final int offset, final int length)
    {
        int result = 1;
        for (int i = offset; i < offset + length; i++)
        {
            result = 31 * result + value[i];
        }

        return result;
    }
}
