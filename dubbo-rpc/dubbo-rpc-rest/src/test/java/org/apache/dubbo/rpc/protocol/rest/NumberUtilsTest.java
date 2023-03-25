/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.rpc.protocol.rest.util.NumberUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NumberUtilsTest {
    public void testParseNumber(String numberStr) {
        int integer = NumberUtils.parseNumber(numberStr, Integer.class);

        Assertions.assertEquals(1, integer);

        integer = NumberUtils.parseNumber(numberStr, int.class);

        Assertions.assertEquals(1, integer);

        long a = NumberUtils.parseNumber(numberStr, Long.class);

        Assertions.assertEquals(1, a);

        a = NumberUtils.parseNumber(numberStr, long.class);

        Assertions.assertEquals(1, a);

        byte b = NumberUtils.parseNumber(numberStr, Byte.class);

        Assertions.assertEquals(1, b);

        b = NumberUtils.parseNumber(numberStr, byte.class);

        Assertions.assertEquals(1, b);

        short c = NumberUtils.parseNumber(numberStr, Short.class);

        Assertions.assertEquals(1, c);

        c = NumberUtils.parseNumber(numberStr, short.class);

        Assertions.assertEquals(1, c);

        BigInteger f = NumberUtils.parseNumber(numberStr, BigInteger.class);

        Assertions.assertEquals(1, f.intValue());


    }

    @Test
    public void testNumberToBytes() {
        byte[] except = {49, 46, 48};
        byte[] bytes = (byte[]) NumberUtils.numberToBytes(Integer.valueOf("1"));

        Assertions.assertArrayEquals(except, bytes);

        bytes = (byte[]) NumberUtils.numberToBytes(NumberUtils.parseNumber("1", int.class));

        Assertions.assertArrayEquals(except, bytes);

        except = new byte[]{49};
        bytes = (byte[]) NumberUtils.numberToBytes(Byte.valueOf("1"));

        Assertions.assertArrayEquals(except, bytes);

        except = new byte[]{49};
        bytes = (byte[]) NumberUtils.numberToBytes(Short.valueOf("1"));

        Assertions.assertArrayEquals(except, bytes);
        except = new byte[]{49};
        Assertions.assertArrayEquals(except, bytes);

        except = new byte[]{49};
        bytes = (byte[]) NumberUtils.numberToBytes(Long.valueOf("1"));

        Assertions.assertArrayEquals(except, bytes);

        except = new byte[]{49};
        bytes = (byte[]) NumberUtils.numberToBytes(BigDecimal.valueOf(1));

        Assertions.assertArrayEquals(except, bytes);


    }

    @Test
    public void testNumberStr() {
        testParseNumber("1");
        testParseNumber("0X0001");
        testParseNumber("0x0001");
        testParseNumber("#1");

    }


    @Test
    public void testUnHexNumber() {
        String numberStr = "1";
        double e = NumberUtils.parseNumber(numberStr, Double.class);

        Assertions.assertEquals(1.0, e);

        e = NumberUtils.parseNumber(numberStr, double.class);

        Assertions.assertEquals(1.0, e);

        BigDecimal g = NumberUtils.parseNumber(numberStr, BigDecimal.class);

        Assertions.assertEquals(1, g.intValue());

        int integer = NumberUtils.parseNumber(numberStr, int.class);

        Assertions.assertEquals(1, integer);
    }

    @Test
    public void testNegative() {

        Integer integer = NumberUtils.parseNumber("-0X1", int.class);
        Assertions.assertEquals(-1, integer);

        BigInteger bigInteger = NumberUtils.parseNumber("-0X1", BigInteger.class);
        Assertions.assertEquals(-1, bigInteger.intValue());
    }

    @Test
    public void testException() {

        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
            Object abc = NumberUtils.parseNumber("abc", Object.class);

        });

        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
            Object abc = NumberUtils.parseNumber(null, Object.class);

        });

        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
            Object abc = NumberUtils.parseNumber("1", null);

        });
    }

}
