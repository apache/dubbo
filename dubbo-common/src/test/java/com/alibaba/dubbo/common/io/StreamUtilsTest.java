/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.io;

import org.junit.Test;

import java.io.InputStream;
import java.io.PushbackInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author ding.lid
 */
public class StreamUtilsTest {

    @Test
    public void testMarkSupportedInputStream() throws Exception {
        InputStream is = StreamUtilsTest.class.getResourceAsStream("/StreamUtilsTest.txt");
        assertEquals(10, is.available());

        is = new PushbackInputStream(is);
        assertEquals(10, is.available());
        assertFalse(is.markSupported());

        is = StreamUtils.markSupportedInputStream(is);
        assertEquals(10, is.available());

        is.mark(0);
        assertEquals((int) '0', is.read());
        assertEquals((int) '1', is.read());

        is.reset();
        assertEquals((int) '0', is.read());
        assertEquals((int) '1', is.read());
        assertEquals((int) '2', is.read());

        is.mark(0);
        assertEquals((int) '3', is.read());
        assertEquals((int) '4', is.read());
        assertEquals((int) '5', is.read());

        is.reset();
        assertEquals((int) '3', is.read());
        assertEquals((int) '4', is.read());

        is.mark(0);
        assertEquals((int) '5', is.read());
        assertEquals((int) '6', is.read());

        is.reset();
        assertEquals((int) '5', is.read());
        assertEquals((int) '6', is.read());
        assertEquals((int) '7', is.read());
        assertEquals((int) '8', is.read());
        assertEquals((int) '9', is.read());
        assertEquals(-1, is.read());
        assertEquals(-1, is.read());

        is.mark(0);
        assertEquals(-1, is.read());
        assertEquals(-1, is.read());

        is.reset();
        assertEquals(-1, is.read());
        assertEquals(-1, is.read());
    }
}