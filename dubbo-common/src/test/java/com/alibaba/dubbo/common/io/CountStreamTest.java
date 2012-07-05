/*
 * Copyright 1999-2011 Alibaba Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.dubbo.common.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.alibaba.dubbo.common.io.CountInputStream;
import com.alibaba.dubbo.common.io.CountOutputStream;

import junit.framework.Assert;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class CountStreamTest {

    class FakeInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            return 1;
        }
    }

    @Test
    public void testCountInputStream() throws Exception {
        CountInputStream inputStream = new CountInputStream(new FakeInputStream());
        long read = 0L;
        for(int i = 0; i < 100; i++) {
            inputStream.read();
            ++read;
        }

        byte[] bytes = new byte[100];
        for(int i = 0; i < 10; i++) {
            inputStream.read(bytes);
            read += bytes.length;
        }

        for(int i = 0; i < 10; i++) {
            inputStream.read(bytes, 0, i+1);
            read += i;
            read += 1;
        }

        for(int i = 0; i < 10; i++) {
            long skiped = inputStream.skip(10);
            read += skiped;
        }

        Assert.assertEquals(read, inputStream.getReadBytes());
    }

    @Test
    public void testCountOutputStream() throws Exception {
        CountOutputStream out = new CountOutputStream(new ByteArrayOutputStream(1024));
        long written = 0L;

        for(int i = 0; i < 100; i++) {
            out.write(1);
            ++written;
        }

        byte[] bytes = new byte[100];
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = 1;
        }
        for(int i = 0; i < 100; i++) {
            out.write(bytes);
            written += bytes.length;
        }

        for(int i = 0; i < 100; i++) {
            out.write(bytes, 0, 50);
            written += 50;
        }

        Assert.assertEquals(written, out.getWriteBytes());
    }

}
