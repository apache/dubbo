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
package org.apache.dubbo.common.serialize.avro;


import org.apache.dubbo.common.serialize.model.Person;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;


public class AvroObjectInputOutputTest {
    private AvroObjectInput avroObjectInput;
    private AvroObjectOutput avroObjectOutput;

    private PipedOutputStream pos;
    private PipedInputStream pis;

    @BeforeEach
    public void setup() throws IOException {
        pis = new PipedInputStream();
        pos = new PipedOutputStream();
        pis.connect(pos);

        avroObjectOutput = new AvroObjectOutput(pos);
        avroObjectInput = new AvroObjectInput(pis);
    }

    @AfterEach
    public void clean() throws IOException {
        if (pos != null) {
            pos.close();
            pos = null;
        }
        if (pis != null) {
            pis.close();
            pis = null;
        }
    }

    @Test
    public void testWriteReadBool() throws IOException, InterruptedException {
        avroObjectOutput.writeBool(true);
        avroObjectOutput.flushBuffer();
        pos.close();

        boolean result = avroObjectInput.readBool();
        assertThat(result, is(true));
    }

    @Test
    public void testWriteReadByte() throws IOException {
        avroObjectOutput.writeByte((byte) 'a');
        avroObjectOutput.flushBuffer();
        pos.close();

        Byte result = avroObjectInput.readByte();

        assertThat(result, is((byte) 'a'));
    }

    @Test
    public void testWriteReadBytes() throws IOException {
        avroObjectOutput.writeBytes("123456".getBytes());
        avroObjectOutput.flushBuffer();
        pos.close();

        byte[] result = avroObjectInput.readBytes();

        assertThat(result, is("123456".getBytes()));
    }

    @Test
    public void testWriteReadShort() throws IOException {
        avroObjectOutput.writeShort((short) 1);
        avroObjectOutput.flushBuffer();
        pos.close();

        short result = avroObjectInput.readShort();

        assertThat(result, is((short) 1));
    }

    @Test
    public void testWriteReadInt() throws IOException {
        avroObjectOutput.writeInt(1);
        avroObjectOutput.flushBuffer();
        pos.close();

        Integer result = avroObjectInput.readInt();

        assertThat(result, is(1));
    }

    @Test
    public void testReadDouble() throws IOException {
        avroObjectOutput.writeDouble(3.14d);
        avroObjectOutput.flushBuffer();
        pos.close();

        Double result = avroObjectInput.readDouble();

        assertThat(result, is(3.14d));
    }

    @Test
    public void testReadLong() throws IOException {
        avroObjectOutput.writeLong(10L);
        avroObjectOutput.flushBuffer();
        pos.close();

        Long result = avroObjectInput.readLong();

        assertThat(result, is(10L));
    }

    @Test
    public void testWriteReadFloat() throws IOException {
        avroObjectOutput.writeFloat(1.66f);
        avroObjectOutput.flushBuffer();
        pos.close();

        Float result = avroObjectInput.readFloat();

        assertThat(result, is(1.66F));
    }

    @Test
    public void testWriteReadUTF() throws IOException {
        avroObjectOutput.writeUTF("wording");
        avroObjectOutput.flushBuffer();
        pos.close();

        String result = avroObjectInput.readUTF();

        assertThat(result, is("wording"));
    }

    @Test
    public void testWriteReadObject() throws IOException, ClassNotFoundException {
        Person p = new Person();
        p.setAge(30);
        p.setName("abc");

        avroObjectOutput.writeObject(p);
        avroObjectOutput.flushBuffer();
        pos.close();

        Person result = avroObjectInput.readObject(Person.class);

        assertThat(result, not(nullValue()));
        assertThat(result.getName(), is("abc"));
        assertThat(result.getAge(), is(30));
    }

    @Test
    public void testWriteReadObjectWithoutClass() throws IOException, ClassNotFoundException {
        Person p = new Person();
        p.setAge(30);
        p.setName("abc");

        avroObjectOutput.writeObject(p);
        avroObjectOutput.flushBuffer();
        pos.close();

        //这里会丢失所有信息
        Object result = avroObjectInput.readObject();

        assertThat(result, not(nullValue()));
//		assertThat(result.getName(), is("abc"));
//		assertThat(result.getAge(), is(30));
    }
}
