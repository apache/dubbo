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

import org.apache.dubbo.common.serialize.model.person.Phone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractSerializationPersonOkTest extends AbstractSerializationTest {
    @Test
    public void test_Phone() throws Exception {
        assertObject(new Phone());
    }

    @Test
    public void test_Person_withType() throws Exception {
        assertObjectWithType(new Phone(), Phone.class);
    }

    @Test
    public void test_PersonList() throws Exception {
        List<Phone> args = new ArrayList<Phone>();
        args.add(new Phone());

        assertObject(args);
    }

    @Test
    public void test_PersonSet() throws Exception {
        Set<Phone> args = new HashSet<Phone>();
        args.add(new Phone());

        assertObject(args);
    }

    @Test
    public void test_IntPersonMap() throws Exception {
        Map<Integer, Phone> args = new HashMap<Integer, Phone>();
        args.put(1, new Phone());

        assertObject(args);
    }

    @Test
    public void test_StringPersonMap() throws Exception {
        Map<String, Phone> args = new HashMap<String, Phone>();
        args.put("1", new Phone());

        assertObject(args);
    }

    @Test
    public void test_StringPersonListMap() throws Exception {
        Map<String, List<Phone>> args = new HashMap<String, List<Phone>>();

        List<Phone> sublist = new ArrayList<Phone>();
        sublist.add(new Phone());
        args.put("1", sublist);

        assertObject(args);
    }

    @Test
    public void test_PersonListList() throws Exception {
        List<List<Phone>> args = new ArrayList<List<Phone>>();
        List<Phone> sublist = new ArrayList<Phone>();
        sublist.add(new Phone());
        args.add(sublist);

        assertObject(args);
    }
}