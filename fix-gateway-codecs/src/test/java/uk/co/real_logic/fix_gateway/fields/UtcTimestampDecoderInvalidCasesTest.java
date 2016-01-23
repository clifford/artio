/*
 * Copyright 2015 Real Logic Ltd.
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
package uk.co.real_logic.fix_gateway.fields;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import uk.co.real_logic.fix_gateway.util.AsciiBuffer;
import uk.co.real_logic.fix_gateway.util.MutableAsciiBuffer;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.US_ASCII;

@RunWith(Parameterized.class)
public class UtcTimestampDecoderInvalidCasesTest
{
    private final String timestamp;

    @Parameters
    public static Iterable<Object> data()
    {
        return Arrays.asList(
            new String[] {"-0010101-00:00:00"},
            new String[] {"00000001-00:00:00"},
            new String[] {"00000100-00:00:00"},
            new String[] {"00001301-00:00:00"},
            new String[] {"00000132-00:00:00"},
            new String[] {"00000101-24:00:00"},
            new String[] {"00000101-00:60:00"},
            new String[] {"00000101-00:00:61"}
        );
    }

    public UtcTimestampDecoderInvalidCasesTest(final String timestamp)
    {
        this.timestamp = timestamp;
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotParseTimestamp()
    {
        final AsciiBuffer timestampBytes = new MutableAsciiBuffer(timestamp.getBytes(US_ASCII));
        UtcTimestampDecoder.decode(timestampBytes, 0, timestamp.length());
    }
}
