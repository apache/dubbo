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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
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
     
        Object generalize = PojoUtils.generalize(map);
        @SuppressWarnings("unchecked")
        Map<String, Object> ret = (Map<String, Object>) PojoUtils.realize(generalize, Map.class);
        
        assertEquals("v", ret.get("k"));
        assertSame(ret, ret.get("m"));
    }
    
    @Ignore
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
        Map<String, Object> realize = (Map<String, Object>) PojoUtils.realize(generalize, Map.class);
        
        Parent parent = (Parent) realize.get("k");
        
        assertEquals(10, parent.getAge());
        assertEquals("jerry", parent.getName());
        
        assertEquals("haha", parent.getChild().getToy());
        assertSame(parent, parent.getChild().getParent());
    }
    
    @Ignore
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
        List<Object> realize = (List<Object>) PojoUtils.realize(generalize, List.class);
        
        Parent parent = (Parent) realize.get(0);
        
        assertEquals(10, parent.getAge());
        assertEquals("jerry", parent.getName());
        
        assertEquals("haha", parent.getChild().getToy());
        assertSame(parent, parent.getChild().getParent());
    }
    
    @Ignore
    @Test
    public void test_PojoInList() throws Exception {
        Parent p = new Parent();
        p.setAge(10);
        p.setName("jerry");
        
        List<Object> list = new ArrayList<Object>();
        list.add(p);
        
        Object generalize = PojoUtils.generalize(list);
        @SuppressWarnings("unchecked")
        List<Object> realize = (List<Object>) PojoUtils.realize(generalize, List.class);
        
        Parent parent = (Parent) realize.get(0);
        
        assertEquals(10, parent.getAge());
        assertEquals("jerry", parent.getName());
    }

    public void setLong(long l){}
    
    public void setInt(int l){}
    
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
}