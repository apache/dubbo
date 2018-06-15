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

package org.apache.dubbo.compatible.serialization;

import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class SerializationTest {

    private MySerialization mySerialization;

    private MyObjectOutput myObjectOutput;
    private MyObjectInput myObjectInput;
    private ByteArrayOutputStream byteArrayOutputStream;
    private ByteArrayInputStream byteArrayInputStream;

    @Before
    public void setUp() throws Exception {
        this.mySerialization = new MySerialization();

        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.myObjectOutput = new MyObjectOutput(byteArrayOutputStream);
    }

    @Test
    public void testContentType() {
        assertThat(mySerialization.getContentType(), is("x-application/my"));
    }

    @Test
    public void testContentTypeId() {
        assertThat(mySerialization.getContentTypeId(), is((byte) 101));
    }

    @Test
    public void testObjectOutput() throws IOException {
        ObjectOutput objectOutput = mySerialization.serialize(null, mock(OutputStream.class));
        assertThat(objectOutput, Matchers.<ObjectOutput>instanceOf(MyObjectOutput.class));
    }

    @Test
    public void testObjectInput() throws IOException {
        ObjectInput objectInput = mySerialization.deserialize(null, mock(InputStream.class));
        assertThat(objectInput, Matchers.<ObjectInput>instanceOf(MyObjectInput.class));
    }

    @Test
    public void testWriteUTF() throws IOException {
        myObjectOutput.writeUTF("Pace");
        myObjectOutput.writeUTF("和平");
        myObjectOutput.writeUTF(" Мир");
        flushToInput();

        assertThat(myObjectInput.readUTF(), CoreMatchers.is("Pace"));
        assertThat(myObjectInput.readUTF(), CoreMatchers.is("和平"));
        assertThat(myObjectInput.readUTF(), CoreMatchers.is(" Мир"));
    }

    private void flushToInput() throws IOException {
        this.myObjectOutput.flushBuffer();
        this.byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.myObjectInput = new MyObjectInput(byteArrayInputStream);
    }
}