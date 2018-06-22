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
package org.apache.dubbo.common.io;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

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

    @Test
    public void testLimitedInputStream() throws Exception {
        InputStream is = StreamUtilsTest.class.getResourceAsStream("/StreamUtilsTest.txt");
        assertThat(10, is(is.available()));

        is = StreamUtils.limitedInputStream(is, 2);
        assertThat(2, is(is.available()));
        assertThat(is.markSupported(), is(true));

        is.mark(0);
        assertEquals((int) '0', is.read());
        assertEquals((int) '1', is.read());
        assertEquals(-1, is.read());

        is.reset();
        is.skip(1);
        assertEquals((int) '1', is.read());

        is.reset();
        is.skip(-1);
        assertEquals((int) '0', is.read());

        is.reset();
        byte[] bytes = new byte[2];
        int read = is.read(bytes, 1, 1);
        assertThat(read, is(1));

        is.reset();
        StreamUtils.skipUnusedStream(is);
        assertEquals(-1, is.read());

        is.close();
    }

    @Test(expected = IOException.class)
    public void testMarkInputSupport() throws IOException {
        InputStream is = StreamUtilsTest.class.getResourceAsStream("/StreamUtilsTest.txt");
        is = StreamUtils.markSupportedInputStream(new PushbackInputStream(is), 1);

        is.mark(1);
        int read = is.read();
        assertThat(read, is((int) '0'));

        is.skip(1);
        is.read();
    }

    @Test
    public void testSkipForOriginMarkSupportInput() {
        InputStream is = StreamUtilsTest.class.getResourceAsStream("/StreamUtilsTest.txt");
        InputStream newIs = StreamUtils.markSupportedInputStream(is, 1);

        assertThat(newIs, is(is));
    }

    @Test(expected = NullPointerException.class)
    public void testReadEmptyByteArray() throws IOException {
        InputStream is = StreamUtilsTest.class.getResourceAsStream("/StreamUtilsTest.txt");
        is = StreamUtils.limitedInputStream(is, 2);
        is.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadWithWrongOffset() throws IOException {
        InputStream is = StreamUtilsTest.class.getResourceAsStream("/StreamUtilsTest.txt");
        is = StreamUtils.limitedInputStream(is, 2);
        is.read(new byte[1], -1, 1);
    }
}