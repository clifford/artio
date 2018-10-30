/*
 * Copyright 2015-2018 Real Logic Ltd, Adaptive Financial Consulting Ltd.
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
package uk.co.real_logic.artio.dictionary;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CharArrayMapTest
{
    @Test
    public void shouldReturnTrueIfKeyIsPresent()
    {
        //Given
        final Map<String, String> buildFrom = new HashMap<>();
        buildFrom.put("0", "_0");
        buildFrom.put("A", "_A");
        buildFrom.put("AA", "_AAA");
        final CharArrayMap<String> charArrayMap = new CharArrayMap<>(buildFrom);

        //When / Then
        assertTrue(charArrayMap.containsKey(new char[] {'0', ' ', 'A', 'A'}, 0, 1));
        assertTrue(charArrayMap.containsKey(new char[] {'0', ' ', 'A', 'A'}, 2, 2));
    }

    @Test
    public void shouldReturnFalseIfKeyIsNotPresent()
    {
        //Given
        final Map<String, String> buildFrom = new HashMap<>();
        buildFrom.put("0", "_0");
        buildFrom.put("A", "_A");
        buildFrom.put("AA", "_AAA");
        final CharArrayMap<String> charArrayMap = new CharArrayMap<>(buildFrom);

        //When / Then
        assertFalse(charArrayMap.containsKey(new char[] {'0', ' ', 'A', 'B'}, 2, 2));
    }
}