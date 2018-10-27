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
package org.apache.dubbo.common.serialize.gson;
import org.apache.dubbo.common.serialize.base.AbstractSerializationPersonOkTest;
import org.apache.dubbo.common.serialize.model.Person;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GsonJsonObjectInputTest  {
    private GsonJsonObjectInput gsonJsonObjectInput;

    @Test
    public void testReadBool() throws IOException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new ByteArrayInputStream("true".getBytes()));
        boolean result = gsonJsonObjectInput.readBool();

        assertThat(result, is(true));

        gsonJsonObjectInput = new GsonJsonObjectInput(new StringReader("false"));
        result = gsonJsonObjectInput.readBool();

        assertThat(result, is(false));
    }

    @Test
    public void testReadByte() throws IOException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new ByteArrayInputStream("123".getBytes()));
        Byte result = gsonJsonObjectInput.readByte();

        assertThat(result, is(Byte.parseByte("123")));
    }

    @Test
    public void testReadBytes() throws IOException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new ByteArrayInputStream("123456".getBytes()));
        byte[] result = gsonJsonObjectInput.readBytes();

        assertThat(result, is("123456".getBytes()));
    }

    @Test
    public void testReadShort() throws IOException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new StringReader("1"));
        short result = gsonJsonObjectInput.readShort();

        assertThat(result, is((short) 1));
    }

    @Test
    public void testReadInt() throws IOException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new StringReader("1"));
        Integer result = gsonJsonObjectInput.readInt();

        assertThat(result, is(1));
    }

    @Test
    public void testReadDouble() throws IOException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new StringReader("1.88"));
        Double result = gsonJsonObjectInput.readDouble();

        assertThat(result, is(1.88d));
    }

    @Test
    public void testReadLong() throws IOException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new StringReader("10"));
        Long result = gsonJsonObjectInput.readLong();

        assertThat(result, is(10L));
    }

    @Test
    public void testReadFloat() throws IOException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new StringReader("1.66"));
        Float result = gsonJsonObjectInput.readFloat();

        assertThat(result, is(1.66F));
    }

    @Test
    public void testReadUTF() throws IOException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new StringReader("\"wording\""));
        String result = gsonJsonObjectInput.readUTF();

        assertThat(result, is("wording"));
    }

    @Test
    public void testReadObject() throws IOException, ClassNotFoundException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new StringReader("{ \"name\":\"John\", \"age\":30 }"));
        Person result = gsonJsonObjectInput.readObject(Person.class);

        assertThat(result, not(nullValue()));
        assertThat(result.getName(), is("John"));
        assertThat(result.getAge(), is(30));
    }

    @Test(expected = EOFException.class)
    public void testEmptyLine() throws IOException, ClassNotFoundException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new StringReader(""));

        gsonJsonObjectInput.readObject();
    }

    @Test(expected = EOFException.class)
    public void testEmptySpace() throws IOException, ClassNotFoundException {
        gsonJsonObjectInput = new GsonJsonObjectInput(new StringReader("  "));
        gsonJsonObjectInput.readObject();
    }


}