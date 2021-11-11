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
package org.apache.dubbo.common.serialize.base;

import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.model.Person;

import org.junit.jupiter.api.Test;

import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractSerializationPersonFailTest extends AbstractSerializationTest {

    protected static final String FAIL_STRING = "Serialized class org.apache.dubbo.common.serialize.model.Person must implement java.io.Serializable";

    @Test
    public void test_Person() throws Exception {
        try {
            ObjectOutput objectOutput = serialization.serialize(url, byteArrayOutputStream);
            objectOutput.writeObject(new Person());
            fail();
        } catch (NotSerializableException expected) {
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString(FAIL_STRING));
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
            assertThat(expected.getMessage(), containsString(FAIL_STRING));
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
            System.out.println("--------" + expected.getMessage());
            assertThat(expected.getMessage(), containsString(FAIL_STRING));
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
            assertThat(expected.getMessage(), containsString(FAIL_STRING));
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
            assertThat(expected.getMessage(), containsString(FAIL_STRING));
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
            assertThat(expected.getMessage(), containsString(FAIL_STRING));
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
            assertThat(expected.getMessage(), containsString(FAIL_STRING));
        }
    }
}