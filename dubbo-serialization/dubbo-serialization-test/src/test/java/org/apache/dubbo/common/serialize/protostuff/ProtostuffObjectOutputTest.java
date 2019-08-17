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
package org.apache.dubbo.common.serialize.protostuff;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.dubbo.common.serialize.model.SerializablePerson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProtostuffObjectOutputTest {

    private ByteArrayOutputStream byteArrayOutputStream;
    private ProtostuffObjectOutput protostuffObjectOutput;
    private ProtostuffObjectInput protostuffObjectInput;
    private ByteArrayInputStream byteArrayInputStream;

    @BeforeEach
    public void setUp() throws Exception {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.protostuffObjectOutput = new ProtostuffObjectOutput(byteArrayOutputStream);
    }

    @Test
    public void testWriteObjectNull() throws IOException, ClassNotFoundException {
        this.protostuffObjectOutput.writeObject(null);
        this.flushToInput();

        assertThat(protostuffObjectInput.readObject(), nullValue());
    }

    @Test
    public void testSerializeTimestamp() throws IOException, ClassNotFoundException {
        Timestamp originTime = new Timestamp(System.currentTimeMillis());
        this.protostuffObjectOutput.writeObject(originTime);
        this.flushToInput();

        Timestamp serializedTime = protostuffObjectInput.readObject(Timestamp.class);
        assertThat(serializedTime, is(originTime));
    }

    @Test
    public void testSerializeSqlDate() throws IOException, ClassNotFoundException {
        java.sql.Date originTime = new java.sql.Date(System.currentTimeMillis());
        this.protostuffObjectOutput.writeObject(originTime);
        this.flushToInput();

        java.sql.Date serializedTime = protostuffObjectInput.readObject(java.sql.Date.class);
        assertThat(serializedTime, is(originTime));
    }

    @Test
    public void testSerializeSqlTime() throws IOException, ClassNotFoundException {
        java.sql.Time originTime = new java.sql.Time(System.currentTimeMillis());
        this.protostuffObjectOutput.writeObject(originTime);
        this.flushToInput();

        java.sql.Time serializedTime = protostuffObjectInput.readObject(java.sql.Time.class);
        assertThat(serializedTime, is(originTime));
    }

    @Test
    public void testSerializeDate() throws IOException, ClassNotFoundException {
        Date originTime = new Date();
        this.protostuffObjectOutput.writeObject(originTime);
        this.flushToInput();

        Date serializedTime = protostuffObjectInput.readObject(Date.class);
        assertThat(serializedTime, is(originTime));
    }

    @Test
    public void testListObject() throws IOException, ClassNotFoundException {
        List<SerializablePerson> list = new ArrayList<SerializablePerson>();
        list.add(new SerializablePerson());
        list.add(new SerializablePerson());
        list.add(new SerializablePerson());
        SerializablePersonList personList = new SerializablePersonList(list);
        this.protostuffObjectOutput.writeObject(personList);
        this.flushToInput();

        SerializablePersonList serializedTime = protostuffObjectInput.readObject(SerializablePersonList.class);
        assertThat(serializedTime, is(personList));
    }

    private void flushToInput() throws IOException {
        this.protostuffObjectOutput.flushBuffer();
        this.byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.protostuffObjectInput = new ProtostuffObjectInput(byteArrayInputStream);
    }

    private class SerializablePersonList implements Serializable {
        private static final long serialVersionUID = 1L;

        public List<SerializablePerson> personList;

        public SerializablePersonList() {}

        public SerializablePersonList(List<SerializablePerson> list) {
            this.personList = list;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;

            SerializablePersonList list = (SerializablePersonList) obj;
            if (list.personList == null && this.personList == null)
                return true;
            if (list.personList == null || this.personList == null)
                return false;
            if (list.personList.size() != this.personList.size())
                return false;
            for (int i =0; i < this.personList.size(); i++) {
                if (!this.personList.get(i).equals(list.personList.get(i)))
                    return false;
            }
            return true;
        }
    }
}
