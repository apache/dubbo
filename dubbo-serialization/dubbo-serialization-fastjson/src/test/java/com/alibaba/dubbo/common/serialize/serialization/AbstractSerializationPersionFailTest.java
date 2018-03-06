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
package com.alibaba.dubbo.common.serialize.serialization;

import com.alibaba.dubbo.common.model.Person;
import com.alibaba.dubbo.common.serialize.ObjectOutput;

import org.junit.Test;

import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

public abstract class AbstractSerializationPersionFailTest extends AbstractSerializationTest {
    @Test
    public void test_Person() throws Exception {
        try {
            ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
            objectOutput.writeObject(new Person());
            fail();
        } catch (NotSerializableException expected) {
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Serialized class com.alibaba.dubbo.common.model.Person must implement java.io.Serializable"));
        }
    }

    @Test
    public void test_PersonList() throws Exception {
        List<Person> args = new ArrayList<Person>();
        args.add(new Person());
        try {
            ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
            objectOutput.writeObject(args);
            fail();
        } catch (NotSerializableException expected) {
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Serialized class com.alibaba.dubbo.common.model.Person must implement java.io.Serializable"));
        }
    }

    @Test
    public void test_PersonSet() throws Exception {
        Set<Person> args = new HashSet<Person>();
        args.add(new Person());
        try {
            ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
            objectOutput.writeObject(args);
            fail();
        } catch (NotSerializableException expected) {
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Serialized class com.alibaba.dubbo.common.model.Person must implement java.io.Serializable"));
        }
    }

    @Test
    public void test_IntPersonMap() throws Exception {
        Map<Integer, Person> args = new HashMap<Integer, Person>();
        args.put(1, new Person());
        try {
            ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
            objectOutput.writeObject(args);
            fail();
        } catch (NotSerializableException expected) {
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Serialized class com.alibaba.dubbo.common.model.Person must implement java.io.Serializable"));
        }
    }

    @Test
    public void test_StringPersonMap() throws Exception {
        Map<String, Person> args = new HashMap<String, Person>();
        args.put("1", new Person());
        try {
            ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
            objectOutput.writeObject(args);
            fail();
        } catch (NotSerializableException expected) {
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Serialized class com.alibaba.dubbo.common.model.Person must implement java.io.Serializable"));
        }
    }

    @Test
    public void test_StringPersonListMap() throws Exception {
        Map<String, List<Person>> args = new HashMap<String, List<Person>>();

        List<Person> sublist = new ArrayList<Person>();
        sublist.add(new Person());
        args.put("1", sublist);
        try {
            ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
            objectOutput.writeObject(args);
            fail();
        } catch (NotSerializableException expected) {
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Serialized class com.alibaba.dubbo.common.model.Person must implement java.io.Serializable"));
        }
    }

    @Test
    public void test_PersonListList() throws Exception {
        List<List<Person>> args = new ArrayList<List<Person>>();
        List<Person> sublist = new ArrayList<Person>();
        sublist.add(new Person());
        args.add(sublist);
        try {
            ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
            objectOutput.writeObject(args);
            fail();
        } catch (NotSerializableException expected) {
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Serialized class com.alibaba.dubbo.common.model.Person must implement java.io.Serializable"));
        }
    }
}