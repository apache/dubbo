/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.common.utils;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.support.io.TempDirectory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(TempDirectory.class)
public class IOUtilsTest {

    private static String TEXT = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
    private InputStream is;
    private OutputStream os;
    private Reader reader;
    private Writer writer;

    @BeforeEach
    public void setUp() throws Exception {
        is = new ByteArrayInputStream(TEXT.getBytes("UTF-8"));
        os = new ByteArrayOutputStream();
        reader = new StringReader(TEXT);
        writer = new StringWriter();
    }

    @AfterEach
    public void tearDown() throws Exception {
        is.close();
        os.close();
        reader.close();
        writer.close();
    }

    @Test
    public void testWrite1() throws Exception {
        assertThat((int) IOUtils.write(is, os, 16), equalTo(TEXT.length()));
    }

    @Test
    public void testWrite2() throws Exception {
        assertThat((int) IOUtils.write(reader, writer, 16), equalTo(TEXT.length()));
    }

    @Test
    public void testWrite3() throws Exception {
        assertThat((int) IOUtils.write(writer, TEXT), equalTo(TEXT.length()));
    }

    @Test
    public void testWrite4() throws Exception {
        assertThat((int) IOUtils.write(is, os), equalTo(TEXT.length()));
    }

    @Test
    public void testWrite5() throws Exception {
        assertThat((int) IOUtils.write(reader, writer), equalTo(TEXT.length()));
    }

    @Test
    public void testLines(@TempDirectory.TempDir Path tmpDir) throws Exception {
        File file = tmpDir.getFileName().toAbsolutePath().toFile();
        IOUtils.writeLines(file, new String[]{TEXT});
        String[] lines = IOUtils.readLines(file);
        assertThat(lines.length, equalTo(1));
        assertThat(lines[0], equalTo(TEXT));
        tmpDir.getFileName().toAbsolutePath().toFile().delete();
    }

    @Test
    public void testReadLines() throws Exception {
        String[] lines = IOUtils.readLines(is);
        assertThat(lines.length, equalTo(1));
        assertThat(lines[0], equalTo(TEXT));
    }

    @Test
    public void testWriteLines() throws Exception {
        IOUtils.writeLines(os, new String[]{TEXT});
        ByteArrayOutputStream bos = (ByteArrayOutputStream) os;
        assertThat(new String(bos.toByteArray()), equalTo(TEXT + System.lineSeparator()));
    }

    @Test
    public void testRead() throws Exception {
        assertThat(IOUtils.read(reader), equalTo(TEXT));
    }

    @Test
    public void testAppendLines(@TempDirectory.TempDir Path tmpDir) throws Exception {
        File file = tmpDir.getFileName().toAbsolutePath().toFile();
        IOUtils.appendLines(file, new String[]{"a", "b", "c"});
        String[] lines = IOUtils.readLines(file);
        assertThat(lines.length, equalTo(3));
        assertThat(lines[0], equalTo("a"));
        assertThat(lines[1], equalTo("b"));
        assertThat(lines[2], equalTo("c"));
        tmpDir.getFileName().toAbsolutePath().toFile().delete();
    }
}
