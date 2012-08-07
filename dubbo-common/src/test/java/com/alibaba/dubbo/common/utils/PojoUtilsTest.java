/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.dubbo.common.model.Person;
import com.alibaba.dubbo.common.model.SerializablePerson;
import com.alibaba.dubbo.common.model.person.BigPerson;
import com.alibaba.dubbo.common.model.person.FullAddress;
import com.alibaba.dubbo.common.model.person.PersonInfo;
import com.alibaba.dubbo.common.model.person.PersonStatus;
import com.alibaba.dubbo.common.model.person.Phone;

/**
 * @author ding.lid
 */
public class PojoUtilsTest {
    
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
        assertObject(new boolean[] { true, false });
        assertObject(new Boolean[] { true, false, true });

        assertObject(new byte[] { 1, 12, 28, 78 });
        assertObject(new Byte[] { 1, 12, 28, 78 });

        assertObject(new char[] { 'a', '中', '无' });
        assertObject(new Character[] { 'a', '中', '无' });

        assertObject(new short[] { 37, 39, 12 });
        assertObject(new Short[] { 37, 39, 12 });

        assertObject(new int[] { 37, -39, 12456 });
        assertObject(new Integer[] { 37, -39, 12456 });
        
        assertObject(new long[] { 37L, -39L, 123456789L });
        assertObject(new Long[] { 37L, -39L, 123456789L });

        assertObject(new float[] { 37F, -3.14F, 123456.7F });
        assertObject(new Float[] { 37F, -39F, 123456.7F });
        
        assertObject(new double[] { 37D, -3.14D, 123456.7D });
        assertObject(new Double[] { 37D, -39D, 123456.7D});
        

        assertArrayObject(new Boolean[] { true, false, true });

        assertArrayObject(new Byte[] { 1, 12, 28, 78 });

        assertArrayObject(new Character[] { 'a', '中', '无' });

        assertArrayObject(new Short[] { 37, 39, 12 });

        assertArrayObject(new Integer[] { 37, -39, 12456 });
        
        assertArrayObject(new Long[] { 37L, -39L, 123456789L });

        assertArrayObject(new Float[] { 37F, -39F, 123456.7F });
        
        assertArrayObject(new Double[] { 37D, -39D, 123456.7D});
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

    public List<Person> returnListPersonMethod() {return null;}
    public BigPerson returnBigPersonMethod() {return null;}
    public Type getType(String methodName){
        Method method;
        try {
            method = getClass().getDeclaredMethod(methodName, new Class<?>[]{} );
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
        assertObject(list,gtype);
    }

    
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

    @Test
    public void test_total() throws Exception {
        Object generalize = PojoUtils.generalize(bigPerson);
        Type gtype = getType("returnBigPersonMethod");
        Object realize = PojoUtils.realize(generalize, BigPerson.class,gtype);
        assertEquals(bigPerson, realize);
    }

    @Test
    public void test_total_Array() throws Exception {
        Object[] persons = new Object[] { bigPerson, bigPerson, bigPerson };

        Object generalize = PojoUtils.generalize(persons);
        Object[] realize = (Object[]) PojoUtils.realize(generalize, Object[].class);
        assertArrayEquals(persons, realize);
    }
    
    // 循环测试
    
    public static class Parent {
        String name;
        
        int age;
        
        Child child;
        
        public String gender;
        
        public String email;
        
        private String securityEmail;
        
        public void setEmail(String email)  {
            this.securityEmail = email;
        }
        
        public String getEmail() {
            return this.securityEmail;
        }

        public static Parent getNewParent() {
            return new Parent();
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
        String toy;
        
        public String gender;
        
        public int age;

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setAge(int age) {
            this.age = age;
        }
        
        public int getAge() {
            return age;
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

        Parent parent;
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

    public void setLong(long l){}
    
    public void setInt(int l){}
    
    public List<Parent> getListGenericType(){return null;};
    public Map<String, Parent> getMapGenericType(){return null;};
    
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
        
        Parent realize = (Parent)PojoUtils.realize(generalize, Parent.class);
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
        Parent realizedParent = (Parent)PojoUtils.realize(obj, Parent.class);
        Assert.assertEquals(parent.gender, realizedParent.gender);
        Assert.assertEquals(child.gender, parent.getChild().gender);
        Assert.assertEquals(child.age, realizedParent.getChild().getAge());
        Assert.assertEquals(parent.getEmail(), realizedParent.getEmail());
        Assert.assertNull(realizedParent.email);
    }

    public static class TestData {
        private Map<String, Child> children = new HashMap<String, Child>();
        private List<Child> list = new ArrayList<Child>();

        public List<Child> getList() {
            return list;
        }

        public void setList(List<Child> list) {
            if (list != null && !list.isEmpty()) {
                this.list.addAll(list);
            }
        }

        public Map<String, Child> getChildren() {
            return children;
        }

        public void setChildren(Map<String, Child> children) {
            if (children!= null && !children.isEmpty()) {
                this.children.putAll(children);
            }
        }

        public void addChild(Child child) {
            this.children.put(child.getName(), child);
        }
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
        Assert.assertEquals(3, data.getChildren().size());
        Assert.assertTrue(data.getChildren().get("first").getClass() == Child.class);
        Assert.assertEquals(1, data.getList().size());
        Assert.assertTrue(data.getList().get(0).getClass() == Child.class);

        TestData realizadData = (TestData) PojoUtils.realize(obj, TestData.class);
        Assert.assertEquals(data.getChildren().size(), realizadData.getChildren().size());
        Assert.assertEquals(data.getChildren().keySet(), realizadData.getChildren().keySet());
        for(Map.Entry<String, Child> entry : data.getChildren().entrySet()) {
            Child c = realizadData.getChildren().get(entry.getKey());
            Assert.assertNotNull(c);
            Assert.assertEquals(entry.getValue().getName(), c.getName());
            Assert.assertEquals(entry.getValue().getAge(), c.getAge());
        }

        Assert.assertEquals(1, realizadData.getList().size());
        Assert.assertEquals(data.getList().get(0).getName(), realizadData.getList().get(0).getName());
        Assert.assertEquals(data.getList().get(0).getAge(), realizadData.getList().get(0).getAge());
    }

    @Test
    public void testRealize() throws Exception {
        Map<String, String> inputMap = new LinkedHashMap<String, String>();
        inputMap.put("key", "value");
        Object obj = PojoUtils.generalize(inputMap);
        Assert.assertTrue(obj instanceof LinkedHashMap);
        Object outputObject = PojoUtils.realize(inputMap, LinkedHashMap.class);
        System.out.println(outputObject.getClass().getName());
        Assert.assertTrue(outputObject instanceof LinkedHashMap);
    }

    @Test
    public void testRealizeLinkedList() throws Exception {
        LinkedList<Person> input = new LinkedList<Person>();
        Person person = new Person();
        person.setAge(37);
        input.add(person);
        Object obj = PojoUtils.generalize(input);
        Assert.assertTrue(obj instanceof List);
        Assert.assertTrue(input.get(0) instanceof Person);
        Object output = PojoUtils.realize(obj, LinkedList.class);
        Assert.assertTrue(output instanceof LinkedList);
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
        Assert.assertTrue(realizeObject instanceof ListResult);
        ListResult listResult = (ListResult)realizeObject;
        List l = listResult.getResult();
        Assert.assertTrue(l.size() == 1);
        Assert.assertTrue(l.get(0) instanceof Parent);
        Parent realizeParent = (Parent)l.get(0);
        Assert.assertEquals(parent.getName(), realizeParent.getName());
        Assert.assertEquals(parent.getAge(), realizeParent.getAge());
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

    private static Child newChild(String name, int age) {
        Child result = new Child();
        result.setName(name);
        result.setAge(age);
        return result;
    }
}