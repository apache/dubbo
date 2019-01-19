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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.model.Person;
import org.apache.dubbo.common.model.SerializablePerson;
import org.apache.dubbo.common.model.person.BigPerson;
import org.apache.dubbo.common.model.person.FullAddress;
import org.apache.dubbo.common.model.person.PersonInfo;
import org.apache.dubbo.common.model.person.PersonStatus;
import org.apache.dubbo.common.model.person.Phone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PojoUtilsTest {

    BigPerson bigPerson;

    {
        bigPerson = new BigPerson();
        bigPerson.setPersonId("id1");
        bigPerson.setLoginName("name1");
        bigPerson.setStatus(PersonStatus.ENABLED);
        bigPerson.setEmail("abc@123.com");
        bigPerson.setPenName("pname");

        ArrayList<Phone> phones = new ArrayList<Phone>();
        Phone phone1 = new Phone("86", "0571", "11223344", "001");
        Phone phone2 = new Phone("86", "0571", "11223344", "002");
        phones.add(phone1);
        phones.add(phone2);

        PersonInfo pi = new PersonInfo();
        pi.setPhones(phones);
        Phone fax = new Phone("86", "0571", "11223344", null);
        pi.setFax(fax);
        FullAddress addr = new FullAddress("CN", "zj", "1234", "Road1", "333444");
        pi.setFullAddress(addr);
        pi.setMobileNo("1122334455");
        pi.setMale(true);
        pi.setDepartment("b2b");
        pi.setHomepageUrl("www.abc.com");
        pi.setJobTitle("dev");
        pi.setName("name2");

        bigPerson.setInfoProfile(pi);
    }

    private static Child newChild(String name, int age) {
        Child result = new Child();
        result.setName(name);
        result.setAge(age);
        return result;
    }

    public void assertObject(Object data) {
        assertObject(data, null);
    }

    public void assertObject(Object data, Type type) {
        Object generalize = PojoUtils.generalize(data);
        Object realize = PojoUtils.realize(generalize, data.getClass(), type);
        assertEquals(data, realize);
    }

    public <T> void assertArrayObject(T[] data) {
        Object generalize = PojoUtils.generalize(data);
        @SuppressWarnings("unchecked")
        T[] realize = (T[]) PojoUtils.realize(generalize, data.getClass());
        assertArrayEquals(data, realize);
    }

    @Test
    public void test_primitive() throws Exception {
        assertObject(Boolean.TRUE);
        assertObject(Boolean.FALSE);

        assertObject(Byte.valueOf((byte) 78));

        assertObject('a');
        assertObject('中');

        assertObject(Short.valueOf((short) 37));

        assertObject(78);

        assertObject(123456789L);

        assertObject(3.14F);
        assertObject(3.14D);
    }

    @Test
    public void test_pojo() throws Exception {
        assertObject(new Person());
        assertObject(new SerializablePerson());
    }

    @Test
    public void test_Map_List_pojo() throws Exception {
        Map<String, List<Object>> map = new HashMap<String, List<Object>>();

        List<Object> list = new ArrayList<Object>();
        list.add(new Person());
        list.add(new SerializablePerson());

        map.put("k", list);

        Object generalize = PojoUtils.generalize(map);
        Object realize = PojoUtils.realize(generalize, Map.class);
        assertEquals(map, realize);
    }

    @Test
    public void test_PrimitiveArray() throws Exception {
        assertObject(new boolean[]{true, false});
        assertObject(new Boolean[]{true, false, true});

        assertObject(new byte[]{1, 12, 28, 78});
        assertObject(new Byte[]{1, 12, 28, 78});

        assertObject(new char[]{'a', '中', '无'});
        assertObject(new Character[]{'a', '中', '无'});

        assertObject(new short[]{37, 39, 12});
        assertObject(new Short[]{37, 39, 12});

        assertObject(new int[]{37, -39, 12456});
        assertObject(new Integer[]{37, -39, 12456});

        assertObject(new long[]{37L, -39L, 123456789L});
        assertObject(new Long[]{37L, -39L, 123456789L});

        assertObject(new float[]{37F, -3.14F, 123456.7F});
        assertObject(new Float[]{37F, -39F, 123456.7F});

        assertObject(new double[]{37D, -3.14D, 123456.7D});
        assertObject(new Double[]{37D, -39D, 123456.7D});


        assertArrayObject(new Boolean[]{true, false, true});

        assertArrayObject(new Byte[]{1, 12, 28, 78});

        assertArrayObject(new Character[]{'a', '中', '无'});

        assertArrayObject(new Short[]{37, 39, 12});

        assertArrayObject(new Integer[]{37, -39, 12456});

        assertArrayObject(new Long[]{37L, -39L, 123456789L});

        assertArrayObject(new Float[]{37F, -39F, 123456.7F});

        assertArrayObject(new Double[]{37D, -39D, 123456.7D});
    }

    @Test
    public void test_PojoArray() throws Exception {
        Person[] array = new Person[2];
        array[0] = new Person();
        {
            Person person = new Person();
            person.setName("xxxx");
            array[1] = person;
        }
        assertArrayObject(array);
    }

    @Test
    public void testArrayToCollection() throws Exception {
        Person[] array = new Person[2];
        Person person1 = new Person();
        person1.setName("person1");
        Person person2 = new Person();
        person2.setName("person2");
        array[0] = person1;
        array[1] = person2;
        Object o = PojoUtils.realize(PojoUtils.generalize(array), LinkedList.class);
        assertTrue(o instanceof LinkedList);
        assertEquals(((List) o).get(0), person1);
        assertEquals(((List) o).get(1), person2);
    }

    @Test
    public void testCollectionToArray() throws Exception {
        Person person1 = new Person();
        person1.setName("person1");
        Person person2 = new Person();
        person2.setName("person2");
        List<Person> list = new LinkedList<Person>();
        list.add(person1);
        list.add(person2);
        Object o = PojoUtils.realize(PojoUtils.generalize(list), Person[].class);
        assertTrue(o instanceof Person[]);
        assertEquals(((Person[]) o)[0], person1);
        assertEquals(((Person[]) o)[1], person2);
    }

    @Test
    public void testMapToEnum() throws Exception {
        Map map = new HashMap();
        map.put("name", "MONDAY");
        Object o = PojoUtils.realize(map, Day.class);
        assertEquals(o, Day.MONDAY);
    }

    @Test
    public void testGeneralizeEnumArray() throws Exception {
        Object days = new Enum[]{Day.FRIDAY, Day.SATURDAY};
        Object o = PojoUtils.generalize(days);
        assertTrue(o instanceof String[]);
        assertEquals(((String[]) o)[0], "FRIDAY");
        assertEquals(((String[]) o)[1], "SATURDAY");
    }

    @Test
    public void testGeneralizePersons() throws Exception {
        Object persons = new Person[]{new Person(), new Person()};
        Object o = PojoUtils.generalize(persons);
        assertTrue(o instanceof Object[]);
        assertEquals(((Object[]) o).length, 2);
    }

    @Test
    public void testMapToInterface() throws Exception {
        Map map = new HashMap();
        map.put("content", "greeting");
        map.put("from", "dubbo");
        map.put("urgent", true);
        Object o = PojoUtils.realize(map, Message.class);
        Message message = (Message) o;
        assertThat(message.getContent(), equalTo("greeting"));
        assertThat(message.getFrom(), equalTo("dubbo"));
        assertTrue(message.isUrgent());
    }

    @Test
    public void testException() throws Exception {
        Map map = new HashMap();
        map.put("message", "dubbo exception");
        Object o = PojoUtils.realize(map, RuntimeException.class);
        assertEquals(((Throwable) o).getMessage(), "dubbo exception");
    }

    @Test
    public void testIsPojo() throws Exception {
        assertFalse(PojoUtils.isPojo(boolean.class));
        assertFalse(PojoUtils.isPojo(Map.class));
        assertFalse(PojoUtils.isPojo(List.class));
        assertTrue(PojoUtils.isPojo(Person.class));
    }

    public List<Person> returnListPersonMethod() {
        return null;
    }

    public BigPerson returnBigPersonMethod() {
        return null;
    }

    public Type getType(String methodName) {
        Method method;
        try {
            method = getClass().getDeclaredMethod(methodName, new Class<?>[]{});
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Type gtype = method.getGenericReturnType();
        return gtype;
    }

    @Test
    public void test_simpleCollection() throws Exception {
        Type gtype = getType("returnListPersonMethod");
        List<Person> list = new ArrayList<Person>();
        list.add(new Person());
        {
            Person person = new Person();
            person.setName("xxxx");
            list.add(person);
        }
        assertObject(list, gtype);
    }

    @Test
    public void test_total() throws Exception {
        Object generalize = PojoUtils.generalize(bigPerson);
        Type gtype = getType("returnBigPersonMethod");
        Object realize = PojoUtils.realize(generalize, BigPerson.class, gtype);
        assertEquals(bigPerson, realize);
    }

    @Test
    public void test_total_Array() throws Exception {
        Object[] persons = new Object[]{bigPerson, bigPerson, bigPerson};

        Object generalize = PojoUtils.generalize(persons);
        Object[] realize = (Object[]) PojoUtils.realize(generalize, Object[].class);
        assertArrayEquals(persons, realize);
    }

    @Test
    public void test_Loop_pojo() throws Exception {
        Parent p = new Parent();
        p.setAge(10);
        p.setName("jerry");

        Child c = new Child();
        c.setToy("haha");

        p.setChild(c);
        c.setParent(p);

        Object generalize = PojoUtils.generalize(p);
        Parent parent = (Parent) PojoUtils.realize(generalize, Parent.class);

        assertEquals(10, parent.getAge());
        assertEquals("jerry", parent.getName());

        assertEquals("haha", parent.getChild().getToy());
        assertSame(parent, parent.getChild().getParent());
    }

    @Test
    public void test_Loop_Map() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("k", "v");
        map.put("m", map);
        assertSame(map, map.get("m"));
        System.out.println(map);
        Object generalize = PojoUtils.generalize(map);
        System.out.println(generalize);
        @SuppressWarnings("unchecked")
        Map<String, Object> ret = (Map<String, Object>) PojoUtils.realize(generalize, Map.class);
        System.out.println(ret);

        assertEquals("v", ret.get("k"));
        assertSame(ret, ret.get("m"));
    }

    @Test
    public void test_LoopPojoInMap() throws Exception {
        Parent p = new Parent();
        p.setAge(10);
        p.setName("jerry");

        Child c = new Child();
        c.setToy("haha");

        p.setChild(c);
        c.setParent(p);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("k", p);

        Object generalize = PojoUtils.generalize(map);
        @SuppressWarnings("unchecked")
        Map<String, Object> realize = (Map<String, Object>) PojoUtils.realize(generalize, Map.class, getType("getMapGenericType"));

        Parent parent = (Parent) realize.get("k");

        assertEquals(10, parent.getAge());
        assertEquals("jerry", parent.getName());

        assertEquals("haha", parent.getChild().getToy());
        assertSame(parent, parent.getChild().getParent());
    }

    @Test
    public void test_LoopPojoInList() throws Exception {
        Parent p = new Parent();
        p.setAge(10);
        p.setName("jerry");

        Child c = new Child();
        c.setToy("haha");

        p.setChild(c);
        c.setParent(p);

        List<Object> list = new ArrayList<Object>();
        list.add(p);

        Object generalize = PojoUtils.generalize(list);
        @SuppressWarnings("unchecked")
        List<Object> realize = (List<Object>) PojoUtils.realize(generalize, List.class, getType("getListGenericType"));

        Parent parent = (Parent) realize.get(0);

        assertEquals(10, parent.getAge());
        assertEquals("jerry", parent.getName());

        assertEquals("haha", parent.getChild().getToy());
        assertSame(parent, parent.getChild().getParent());

        Object[] objects = PojoUtils.realize(new Object[]{generalize}, new Class[]{List.class}, new Type[]{getType("getListGenericType")});
        assertTrue(((List) objects[0]).get(0) instanceof Parent);
    }

    @Test
    public void test_PojoInList() throws Exception {
        Parent p = new Parent();
        p.setAge(10);
        p.setName("jerry");

        List<Object> list = new ArrayList<Object>();
        list.add(p);

        Object generalize = PojoUtils.generalize(list);
        @SuppressWarnings("unchecked")
        List<Object> realize = (List<Object>) PojoUtils.realize(generalize, List.class, getType("getListGenericType"));

        Parent parent = (Parent) realize.get(0);

        assertEquals(10, parent.getAge());
        assertEquals("jerry", parent.getName());
    }

    public void setLong(long l) {
    }

    public void setInt(int l) {
    }

    public List<Parent> getListGenericType() {
        return null;
    }

    public Map<String, Parent> getMapGenericType() {
        return null;
    }

    // java.lang.IllegalArgumentException: argument type mismatch
    @Test
    public void test_realize_LongPararmter_IllegalArgumentException() throws Exception {
        Method method = PojoUtilsTest.class.getMethod("setLong", long.class);
        assertNotNull(method);

        Object value = PojoUtils.realize("563439743927993", method.getParameterTypes()[0], method.getGenericParameterTypes()[0]);

        method.invoke(new PojoUtilsTest(), value);
    }

    // java.lang.IllegalArgumentException: argument type mismatch
    @Test
    public void test_realize_IntPararmter_IllegalArgumentException() throws Exception {
        Method method = PojoUtilsTest.class.getMethod("setInt", int.class);
        assertNotNull(method);

        Object value = PojoUtils.realize("123", method.getParameterTypes()[0], method.getGenericParameterTypes()[0]);

        method.invoke(new PojoUtilsTest(), value);
    }

    @Test
    public void testStackOverflow() throws Exception {
        Parent parent = Parent.getNewParent();
        parent.setAge(Integer.MAX_VALUE);
        String name = UUID.randomUUID().toString();
        parent.setName(name);
        Object generalize = PojoUtils.generalize(parent);
        assertTrue(generalize instanceof Map);
        Map map = (Map) generalize;
        assertEquals(Integer.MAX_VALUE, map.get("age"));
        assertEquals(name, map.get("name"));

        Parent realize = (Parent) PojoUtils.realize(generalize, Parent.class);
        assertEquals(Integer.MAX_VALUE, realize.getAge());
        assertEquals(name, realize.getName());
    }

    @Test
    public void testGenerializeAndRealizeClass() throws Exception {
        Object generalize = PojoUtils.generalize(Integer.class);
        assertEquals(Integer.class.getName(), generalize);
        Object real = PojoUtils.realize(generalize, Integer.class.getClass());
        assertEquals(Integer.class, real);

        generalize = PojoUtils.generalize(int[].class);
        assertEquals(int[].class.getName(), generalize);
        real = PojoUtils.realize(generalize, int[].class.getClass());
        assertEquals(int[].class, real);
    }

    @Test
    public void testPublicField() throws Exception {
        Parent parent = new Parent();
        parent.gender = "female";
        parent.email = "email@host.com";
        parent.setEmail("securityemail@host.com");
        Child child = new Child();
        parent.setChild(child);
        child.gender = "male";
        child.setAge(20);
        child.setParent(parent);
        Object obj = PojoUtils.generalize(parent);
        Parent realizedParent = (Parent) PojoUtils.realize(obj, Parent.class);
        Assertions.assertEquals(parent.gender, realizedParent.gender);
        Assertions.assertEquals(child.gender, parent.getChild().gender);
        Assertions.assertEquals(child.age, realizedParent.getChild().getAge());
        Assertions.assertEquals(parent.getEmail(), realizedParent.getEmail());
        Assertions.assertNull(realizedParent.email);
    }

    @Test
    public void testMapField() throws Exception {
        TestData data = new TestData();
        Child child = newChild("first", 1);
        data.addChild(child);
        child = newChild("second", 2);
        data.addChild(child);
        child = newChild("third", 3);
        data.addChild(child);

        data.setList(Arrays.asList(newChild("forth", 4)));

        Object obj = PojoUtils.generalize(data);
        Assertions.assertEquals(3, data.getChildren().size());
        assertTrue(data.getChildren().get("first").getClass() == Child.class);
        Assertions.assertEquals(1, data.getList().size());
        assertTrue(data.getList().get(0).getClass() == Child.class);

        TestData realizadData = (TestData) PojoUtils.realize(obj, TestData.class);
        Assertions.assertEquals(data.getChildren().size(), realizadData.getChildren().size());
        Assertions.assertEquals(data.getChildren().keySet(), realizadData.getChildren().keySet());
        for (Map.Entry<String, Child> entry : data.getChildren().entrySet()) {
            Child c = realizadData.getChildren().get(entry.getKey());
            Assertions.assertNotNull(c);
            Assertions.assertEquals(entry.getValue().getName(), c.getName());
            Assertions.assertEquals(entry.getValue().getAge(), c.getAge());
        }

        Assertions.assertEquals(1, realizadData.getList().size());
        Assertions.assertEquals(data.getList().get(0).getName(), realizadData.getList().get(0).getName());
        Assertions.assertEquals(data.getList().get(0).getAge(), realizadData.getList().get(0).getAge());
    }

    @Test
    public void testRealize() throws Exception {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("key", "value");
        Object obj = PojoUtils.generalize(map);
        assertTrue(obj instanceof LinkedHashMap);
        Object outputObject = PojoUtils.realize(map, LinkedHashMap.class);
        assertTrue(outputObject instanceof LinkedHashMap);
        Object[] objects = PojoUtils.realize(new Object[]{map}, new Class[]{LinkedHashMap.class});
        assertTrue(objects[0] instanceof LinkedHashMap);
        assertEquals(objects[0], outputObject);
    }

    @Test
    public void testRealizeLinkedList() throws Exception {
        LinkedList<Person> input = new LinkedList<Person>();
        Person person = new Person();
        person.setAge(37);
        input.add(person);
        Object obj = PojoUtils.generalize(input);
        assertTrue(obj instanceof List);
        assertTrue(input.get(0) instanceof Person);
        Object output = PojoUtils.realize(obj, LinkedList.class);
        assertTrue(output instanceof LinkedList);
    }

    @Test
    public void testPojoList() throws Exception {
        ListResult<Parent> result = new ListResult<Parent>();
        List<Parent> list = new ArrayList<Parent>();
        Parent parent = new Parent();
        parent.setAge(Integer.MAX_VALUE);
        parent.setName("zhangsan");
        list.add(parent);
        result.setResult(list);

        Object generializeObject = PojoUtils.generalize(result);
        Object realizeObject = PojoUtils.realize(generializeObject, ListResult.class);
        assertTrue(realizeObject instanceof ListResult);
        ListResult listResult = (ListResult) realizeObject;
        List l = listResult.getResult();
        assertTrue(l.size() == 1);
        assertTrue(l.get(0) instanceof Parent);
        Parent realizeParent = (Parent) l.get(0);
        Assertions.assertEquals(parent.getName(), realizeParent.getName());
        Assertions.assertEquals(parent.getAge(), realizeParent.getAge());
    }

    @Test
    public void testListPojoListPojo() throws Exception {
        InnerPojo<Parent> parentList = new InnerPojo<Parent>();
        Parent parent = new Parent();
        parent.setName("zhangsan");
        parent.setAge(Integer.MAX_VALUE);
        parentList.setList(Arrays.asList(parent));

        ListResult<InnerPojo<Parent>> list = new ListResult<InnerPojo<Parent>>();
        list.setResult(Arrays.asList(parentList));

        Object generializeObject = PojoUtils.generalize(list);
        Object realizeObject = PojoUtils.realize(generializeObject, ListResult.class);

        assertTrue(realizeObject instanceof ListResult);
        ListResult realizeList = (ListResult) realizeObject;
        List realizeInnerList = realizeList.getResult();
        Assertions.assertEquals(1, realizeInnerList.size());
        assertTrue(realizeInnerList.get(0) instanceof InnerPojo);
        InnerPojo realizeParentList = (InnerPojo) realizeInnerList.get(0);
        Assertions.assertEquals(1, realizeParentList.getList().size());
        assertTrue(realizeParentList.getList().get(0) instanceof Parent);
        Parent realizeParent = (Parent) realizeParentList.getList().get(0);
        Assertions.assertEquals(parent.getName(), realizeParent.getName());
        Assertions.assertEquals(parent.getAge(), realizeParent.getAge());
    }

    @Test
    public void testDateTimeTimestamp() throws Exception {
        String dateStr = "2018-09-12";
        String timeStr = "10:12:33";
        String dateTimeStr = "2018-09-12 10:12:33";
        String[] dateFormat = new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "HH:mm:ss"};

        //java.util.Date
        Object date = PojoUtils.realize(dateTimeStr, Date.class, (Type) Date.class);
        assertEquals(Date.class, date.getClass());
        assertEquals(dateTimeStr, new SimpleDateFormat(dateFormat[0]).format(date));

        //java.sql.Time
        Object time = PojoUtils.realize(dateTimeStr, java.sql.Time.class, (Type) java.sql.Time.class);
        assertEquals(java.sql.Time.class, time.getClass());
        assertEquals(timeStr, new SimpleDateFormat(dateFormat[2]).format(time));

        //java.sql.Date
        Object sqlDate = PojoUtils.realize(dateTimeStr, java.sql.Date.class, (Type) java.sql.Date.class);
        assertEquals(java.sql.Date.class, sqlDate.getClass());
        assertEquals(dateStr, new SimpleDateFormat(dateFormat[1]).format(sqlDate));

        //java.sql.Timestamp
        Object timestamp = PojoUtils.realize(dateTimeStr, java.sql.Timestamp.class, (Type) java.sql.Timestamp.class);
        assertEquals(java.sql.Timestamp.class, timestamp.getClass());
        assertEquals(dateTimeStr, new SimpleDateFormat(dateFormat[0]).format(timestamp));
    }

    public enum Day {
        SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
    }

    public static class Parent {
        public String gender;
        public String email;
        String name;
        int age;
        Child child;
        private String securityEmail;

        public static Parent getNewParent() {
            return new Parent();
        }

        public String getEmail() {
            return this.securityEmail;
        }

        public void setEmail(String email) {
            this.securityEmail = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }
    }

    public static class Child {
        public String gender;
        public int age;
        String toy;
        Parent parent;
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getToy() {
            return toy;
        }

        public void setToy(String toy) {
            this.toy = toy;
        }

        public Parent getParent() {
            return parent;
        }

        public void setParent(Parent parent) {
            this.parent = parent;
        }
    }

    public static class TestData {
        private Map<String, Child> children = new HashMap<String, Child>();
        private List<Child> list = new ArrayList<Child>();

        public List<Child> getList() {
            return list;
        }

        public void setList(List<Child> list) {
            if (CollectionUtils.isNotEmpty(list)) {
                this.list.addAll(list);
            }
        }

        public Map<String, Child> getChildren() {
            return children;
        }

        public void setChildren(Map<String, Child> children) {
            if (CollectionUtils.isNotEmptyMap(children)) {
                this.children.putAll(children);
            }
        }

        public void addChild(Child child) {
            this.children.put(child.getName(), child);
        }
    }

    public static class InnerPojo<T> {
        private List<T> list;

        public List<T> getList() {
            return list;
        }

        public void setList(List<T> list) {
            this.list = list;
        }
    }

    public static class ListResult<T> {
        List<T> result;

        public List<T> getResult() {
            return result;
        }

        public void setResult(List<T> result) {
            this.result = result;
        }
    }

    interface Message {
        String getContent();

        String getFrom();

        boolean isUrgent();
    }
}