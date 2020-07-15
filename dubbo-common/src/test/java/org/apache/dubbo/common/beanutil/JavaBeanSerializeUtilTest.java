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
package org.apache.dubbo.common.beanutil;

import org.apache.dubbo.common.utils.PojoUtilsTest;
import org.apache.dubbo.rpc.model.person.BigPerson;
import org.apache.dubbo.rpc.model.person.FullAddress;
import org.apache.dubbo.rpc.model.person.PersonInfo;
import org.apache.dubbo.rpc.model.person.PersonStatus;
import org.apache.dubbo.rpc.model.person.Phone;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JavaBeanSerializeUtilTest {

    @Test
    public void testSerialize_Primitive() {
        JavaBeanDescriptor descriptor;
        descriptor = JavaBeanSerializeUtil.serialize(Integer.MAX_VALUE);
        Assertions.assertTrue(descriptor.isPrimitiveType());
        Assertions.assertEquals(Integer.MAX_VALUE, descriptor.getPrimitiveProperty());

        Date now = new Date();
        descriptor = JavaBeanSerializeUtil.serialize(now);
        Assertions.assertTrue(descriptor.isPrimitiveType());
        Assertions.assertEquals(now, descriptor.getPrimitiveProperty());
    }

    @Test
    public void testSerialize_Primitive_NUll() {
        JavaBeanDescriptor descriptor;
        descriptor = JavaBeanSerializeUtil.serialize(null);
        Assertions.assertNull(descriptor);
    }

    @Test
    public void testDeserialize_Primitive() {
        JavaBeanDescriptor descriptor = new JavaBeanDescriptor(long.class.getName(), JavaBeanDescriptor.TYPE_PRIMITIVE);
        descriptor.setPrimitiveProperty(Long.MAX_VALUE);
        Assertions.assertEquals(Long.MAX_VALUE, JavaBeanSerializeUtil.deserialize(descriptor));

        BigDecimal decimal = BigDecimal.TEN;
        Assertions.assertEquals(Long.MAX_VALUE, descriptor.setPrimitiveProperty(decimal));
        Assertions.assertEquals(decimal, JavaBeanSerializeUtil.deserialize(descriptor));

        String string = UUID.randomUUID().toString();
        Assertions.assertEquals(decimal, descriptor.setPrimitiveProperty(string));
        Assertions.assertEquals(string, JavaBeanSerializeUtil.deserialize(descriptor));
    }

    @Test
    public void testDeserialize_Primitive0() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            JavaBeanDescriptor descriptor = new JavaBeanDescriptor(long.class.getName(),
                    JavaBeanDescriptor.TYPE_BEAN + 1);
        });
    }

    @Test
    public void testDeserialize_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            JavaBeanDescriptor descriptor = new JavaBeanDescriptor(null, JavaBeanDescriptor.TYPE_BEAN);
        });
    }

    @Test
    public void testDeserialize_containsProperty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            JavaBeanDescriptor descriptor = new JavaBeanDescriptor(long.class.getName(),
                    JavaBeanDescriptor.TYPE_PRIMITIVE);
            descriptor.containsProperty(null);
        });
    }

    @Test
    public void testSetEnumNameProperty() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JavaBeanDescriptor descriptor = new JavaBeanDescriptor(long.class.getName(),
                    JavaBeanDescriptor.TYPE_PRIMITIVE);
            descriptor.setEnumNameProperty(JavaBeanDescriptor.class.getName());
        });

        JavaBeanDescriptor descriptor = new JavaBeanDescriptor(JavaBeanDescriptor.class.getName(),
                JavaBeanDescriptor.TYPE_ENUM);

        String oldValueOrigin = descriptor.setEnumNameProperty(JavaBeanDescriptor.class.getName());
        Assertions.assertNull(oldValueOrigin);

        String oldValueNext = descriptor.setEnumNameProperty(JavaBeanDescriptor.class.getName());
        Assertions.assertEquals(oldValueNext, descriptor.getEnumPropertyName());
    }

    @Test
    public void testGetEnumNameProperty() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JavaBeanDescriptor descriptor = new JavaBeanDescriptor(long.class.getName(),
                    JavaBeanDescriptor.TYPE_PRIMITIVE);
            descriptor.getEnumPropertyName();
        });
    }

    @Test
    public void testSetClassNameProperty() {

        Assertions.assertThrows(IllegalStateException.class, () -> {
            JavaBeanDescriptor descriptor = new JavaBeanDescriptor(long.class.getName(),
                    JavaBeanDescriptor.TYPE_PRIMITIVE);
            descriptor.setClassNameProperty(JavaBeanDescriptor.class.getName());
        });

        JavaBeanDescriptor descriptor = new JavaBeanDescriptor(JavaBeanDescriptor.class.getName(),
                JavaBeanDescriptor.TYPE_CLASS);

        String oldValue1 = descriptor.setClassNameProperty(JavaBeanDescriptor.class.getName());
        Assertions.assertNull(oldValue1);

        String oldValue2 = descriptor.setClassNameProperty(JavaBeanDescriptor.class.getName());
        Assertions.assertEquals(oldValue2, descriptor.getClassNameProperty());
    }

    @Test
    public void testGetClassNameProperty() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JavaBeanDescriptor descriptor = new JavaBeanDescriptor(long.class.getName(),
                    JavaBeanDescriptor.TYPE_PRIMITIVE);
            descriptor.getClassNameProperty();
        });
    }

    @Test
    public void testSetPrimitiveProperty() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JavaBeanDescriptor descriptor = new JavaBeanDescriptor(JavaBeanDescriptor.class.getName(),
                    JavaBeanDescriptor.TYPE_BEAN);
            descriptor.setPrimitiveProperty(JavaBeanDescriptor.class.getName());
        });
    }

    @Test
    public void testGetPrimitiveProperty() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JavaBeanDescriptor descriptor = new JavaBeanDescriptor(JavaBeanDescriptor.class.getName(),
                    JavaBeanDescriptor.TYPE_BEAN);
            descriptor.getPrimitiveProperty();
        });
    }

    @Test
    public void testDeserialize_get_and_set() {
        JavaBeanDescriptor descriptor = new JavaBeanDescriptor(long.class.getName(), JavaBeanDescriptor.TYPE_BEAN);
        descriptor.setType(JavaBeanDescriptor.TYPE_PRIMITIVE);
        Assertions.assertEquals(descriptor.getType(), JavaBeanDescriptor.TYPE_PRIMITIVE);
        descriptor.setClassName(JavaBeanDescriptor.class.getName());
        Assertions.assertEquals(JavaBeanDescriptor.class.getName(), descriptor.getClassName());
    }

    @Test
    public void testSerialize_Array() {
        int[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        JavaBeanDescriptor descriptor = JavaBeanSerializeUtil.serialize(array, JavaBeanAccessor.METHOD);
        Assertions.assertTrue(descriptor.isArrayType());
        Assertions.assertEquals(int.class.getName(), descriptor.getClassName());
        for (int i = 0; i < array.length; i++) {
            Assertions.assertEquals(array[i],
                    ((JavaBeanDescriptor) descriptor.getProperty(i)).getPrimitiveProperty());
        }

        Integer[] integers = new Integer[]{1, 2, 3, 4, null, null, null};
        descriptor = JavaBeanSerializeUtil.serialize(integers, JavaBeanAccessor.METHOD);
        Assertions.assertTrue(descriptor.isArrayType());
        Assertions.assertEquals(Integer.class.getName(), descriptor.getClassName());
        Assertions.assertEquals(integers.length, descriptor.propertySize());
        for (int i = 0; i < integers.length; i++) {
            if (integers[i] == null) {
                Assertions.assertSame(integers[i], descriptor.getProperty(i));
            } else {
                Assertions.assertEquals(integers[i], ((JavaBeanDescriptor) descriptor.getProperty(i))
                        .getPrimitiveProperty());
            }
        }

        int[][] second = {{1, 2}, {3, 4}};
        descriptor = JavaBeanSerializeUtil.serialize(second, JavaBeanAccessor.METHOD);
        Assertions.assertTrue(descriptor.isArrayType());
        Assertions.assertEquals(int[].class.getName(), descriptor.getClassName());
        for (int i = 0; i < second.length; i++) {
            for (int j = 0; j < second[i].length; j++) {
                JavaBeanDescriptor item = (((JavaBeanDescriptor) descriptor.getProperty(i)));
                Assertions.assertTrue(item.isArrayType());
                Assertions.assertEquals(int.class.getName(), item.getClassName());
                Assertions.assertEquals(second[i][j], ((JavaBeanDescriptor) item.getProperty(j)).getPrimitiveProperty());
            }
        }

        BigPerson[] persons = new BigPerson[]{createBigPerson(), createBigPerson()};
        descriptor = JavaBeanSerializeUtil.serialize(persons);
        Assertions.assertTrue(descriptor.isArrayType());
        Assertions.assertEquals(BigPerson.class.getName(), descriptor.getClassName());
        for (int i = 0; i < persons.length; i++) {
            assertEqualsBigPerson(persons[i], descriptor.getProperty(i));
        }
    }

    @Test
    public void testConstructorArg() {
        Assertions.assertFalse((boolean) JavaBeanSerializeUtil.getConstructorArg(boolean.class));
        Assertions.assertFalse((boolean) JavaBeanSerializeUtil.getConstructorArg(Boolean.class));
        Assertions.assertEquals((byte) 0, JavaBeanSerializeUtil.getConstructorArg(byte.class));
        Assertions.assertEquals((byte) 0, JavaBeanSerializeUtil.getConstructorArg(Byte.class));
        Assertions.assertEquals((short) 0, JavaBeanSerializeUtil.getConstructorArg(short.class));
        Assertions.assertEquals((short) 0, JavaBeanSerializeUtil.getConstructorArg(Short.class));
        Assertions.assertEquals(0, JavaBeanSerializeUtil.getConstructorArg(int.class));
        Assertions.assertEquals(0, JavaBeanSerializeUtil.getConstructorArg(Integer.class));
        Assertions.assertEquals((long) 0, JavaBeanSerializeUtil.getConstructorArg(long.class));
        Assertions.assertEquals((long) 0, JavaBeanSerializeUtil.getConstructorArg(Long.class));
        Assertions.assertEquals((float) 0, JavaBeanSerializeUtil.getConstructorArg(float.class));
        Assertions.assertEquals((float) 0, JavaBeanSerializeUtil.getConstructorArg(Float.class));
        Assertions.assertEquals((double) 0, JavaBeanSerializeUtil.getConstructorArg(double.class));
        Assertions.assertEquals((double) 0, JavaBeanSerializeUtil.getConstructorArg(Double.class));
        Assertions.assertEquals((char) 0, JavaBeanSerializeUtil.getConstructorArg(char.class));
        Assertions.assertEquals(new Character((char) 0), JavaBeanSerializeUtil.getConstructorArg(Character.class));
        Assertions.assertNull(JavaBeanSerializeUtil.getConstructorArg(JavaBeanSerializeUtil.class));
    }

    @Test
    public void testDeserialize_Array() {
        final int len = 10;
        JavaBeanDescriptor descriptor = new JavaBeanDescriptor(int.class.getName(), JavaBeanDescriptor.TYPE_ARRAY);
        for (int i = 0; i < len; i++) {
            descriptor.setProperty(i, i);
        }

        Object obj = JavaBeanSerializeUtil.deserialize(descriptor);
        Assertions.assertTrue(obj.getClass().isArray());
        Assertions.assertSame(int.class, obj.getClass().getComponentType());
        for (int i = 0; i < len; i++) {
            Assertions.assertEquals(i, Array.get(obj, i));
        }

        descriptor = new JavaBeanDescriptor(int[].class.getName(), JavaBeanDescriptor.TYPE_ARRAY);
        for (int i = 0; i < len; i++) {
            JavaBeanDescriptor innerItem = new JavaBeanDescriptor(int.class.getName(), JavaBeanDescriptor.TYPE_ARRAY);
            for (int j = 0; j < len; j++) {
                innerItem.setProperty(j, j);
            }
            descriptor.setProperty(i, innerItem);
        }
        obj = JavaBeanSerializeUtil.deserialize(descriptor);
        Assertions.assertTrue(obj.getClass().isArray());
        Assertions.assertEquals(int[].class, obj.getClass().getComponentType());
        for (int i = 0; i < len; i++) {
            Object innerItem = Array.get(obj, i);
            Assertions.assertTrue(innerItem.getClass().isArray());
            Assertions.assertEquals(int.class, innerItem.getClass().getComponentType());
            for (int j = 0; j < len; j++) {
                Assertions.assertEquals(j, Array.get(innerItem, j));
            }
        }

        descriptor = new JavaBeanDescriptor(BigPerson[].class.getName(), JavaBeanDescriptor.TYPE_ARRAY);
        JavaBeanDescriptor innerDescriptor = new JavaBeanDescriptor(BigPerson.class.getName(),
                JavaBeanDescriptor.TYPE_ARRAY);
        innerDescriptor.setProperty(0, JavaBeanSerializeUtil.serialize(createBigPerson(), JavaBeanAccessor.METHOD));
        descriptor.setProperty(0, innerDescriptor);

        obj = JavaBeanSerializeUtil.deserialize(descriptor);
        Assertions.assertTrue(obj.getClass().isArray());
        Assertions.assertEquals(BigPerson[].class, obj.getClass().getComponentType());
        Assertions.assertEquals(1, Array.getLength(obj));
        obj = Array.get(obj, 0);
        Assertions.assertTrue(obj.getClass().isArray());
        Assertions.assertEquals(BigPerson.class, obj.getClass().getComponentType());
        Assertions.assertEquals(1, Array.getLength(obj));
        Assertions.assertEquals(createBigPerson(), Array.get(obj, 0));
    }

    @Test
    public void test_Circular_Reference() {
        PojoUtilsTest.Parent parent = new PojoUtilsTest.Parent();
        parent.setAge(Integer.MAX_VALUE);
        parent.setEmail("a@b");
        parent.setName("zhangsan");

        PojoUtilsTest.Child child = new PojoUtilsTest.Child();
        child.setAge(100);
        child.setName("lisi");
        child.setParent(parent);

        parent.setChild(child);

        JavaBeanDescriptor descriptor = JavaBeanSerializeUtil.serialize(parent, JavaBeanAccessor.METHOD);
        Assertions.assertTrue(descriptor.isBeanType());
        assertEqualsPrimitive(parent.getAge(), descriptor.getProperty("age"));
        assertEqualsPrimitive(parent.getName(), descriptor.getProperty("name"));
        assertEqualsPrimitive(parent.getEmail(), descriptor.getProperty("email"));

        JavaBeanDescriptor childDescriptor = (JavaBeanDescriptor) descriptor.getProperty("child");
        Assertions.assertSame(descriptor, childDescriptor.getProperty("parent"));
        assertEqualsPrimitive(child.getName(), childDescriptor.getProperty("name"));
        assertEqualsPrimitive(child.getAge(), childDescriptor.getProperty("age"));
    }

    @Test
    public void testBeanSerialize() {
        Bean bean = new Bean();
        bean.setDate(new Date());
        bean.setStatus(PersonStatus.ENABLED);
        bean.setType(Bean.class);
        bean.setArray(new Phone[]{});

        Collection<Phone> collection = new ArrayList<Phone>();
        bean.setCollection(collection);
        Phone phone = new Phone();
        collection.add(phone);

        Map<String, FullAddress> map = new HashMap<String, FullAddress>();
        FullAddress address = new FullAddress();
        map.put("first", address);
        bean.setAddresses(map);

        JavaBeanDescriptor descriptor = JavaBeanSerializeUtil.serialize(bean, JavaBeanAccessor.METHOD);
        Assertions.assertTrue(descriptor.isBeanType());
        assertEqualsPrimitive(bean.getDate(), descriptor.getProperty("date"));
        assertEqualsEnum(bean.getStatus(), descriptor.getProperty("status"));
        Assertions.assertTrue(((JavaBeanDescriptor) descriptor.getProperty("type")).isClassType());
        Assertions.assertEquals(Bean.class.getName(), ((JavaBeanDescriptor) descriptor.getProperty("type"))
                .getClassNameProperty());
        Assertions.assertTrue(((JavaBeanDescriptor) descriptor.getProperty("array")).isArrayType());
        Assertions.assertEquals(0, ((JavaBeanDescriptor) descriptor.getProperty("array")).propertySize());

        JavaBeanDescriptor property = (JavaBeanDescriptor) descriptor.getProperty("collection");
        Assertions.assertTrue(property.isCollectionType());
        Assertions.assertEquals(1, property.propertySize());
        property = (JavaBeanDescriptor) property.getProperty(0);
        Assertions.assertTrue(property.isBeanType());
        Assertions.assertEquals(Phone.class.getName(), property.getClassName());
        Assertions.assertEquals(0, property.propertySize());

        property = (JavaBeanDescriptor) descriptor.getProperty("addresses");
        Assertions.assertTrue(property.isMapType());
        Assertions.assertEquals(bean.getAddresses().getClass().getName(), property.getClassName());
        Assertions.assertEquals(1, property.propertySize());

        Map.Entry<Object, Object> entry = property.iterator().next();
        Assertions.assertTrue(((JavaBeanDescriptor) entry.getKey()).isPrimitiveType());
        Assertions.assertEquals("first", ((JavaBeanDescriptor) entry.getKey()).getPrimitiveProperty());

        Assertions.assertTrue(((JavaBeanDescriptor) entry.getValue()).isBeanType());
        Assertions.assertEquals(FullAddress.class.getName(), ((JavaBeanDescriptor) entry.getValue()).getClassName());
        Assertions.assertEquals(0, ((JavaBeanDescriptor) entry.getValue()).propertySize());
    }

    @Test
    public void testDeserializeBean() {
        Bean bean = new Bean();
        bean.setDate(new Date());
        bean.setStatus(PersonStatus.ENABLED);
        bean.setType(Bean.class);
        bean.setArray(new Phone[]{});

        Collection<Phone> collection = new ArrayList<Phone>();
        bean.setCollection(collection);
        Phone phone = new Phone();
        collection.add(phone);

        Map<String, FullAddress> map = new HashMap<String, FullAddress>();
        FullAddress address = new FullAddress();
        map.put("first", address);
        bean.setAddresses(map);

        JavaBeanDescriptor beanDescriptor = JavaBeanSerializeUtil.serialize(bean, JavaBeanAccessor.METHOD);
        Object deser = JavaBeanSerializeUtil.deserialize(beanDescriptor);
        Assertions.assertTrue(deser instanceof Bean);
        Bean deserBean = (Bean) deser;
        Assertions.assertEquals(bean.getDate(), deserBean.getDate());
        Assertions.assertEquals(bean.getStatus(), deserBean.getStatus());
        Assertions.assertEquals(bean.getType(), deserBean.getType());
        Assertions.assertEquals(bean.getCollection().size(), deserBean.getCollection().size());
        Assertions.assertEquals(bean.getCollection().iterator().next().getClass(),
                deserBean.getCollection().iterator().next().getClass());
        Assertions.assertEquals(bean.getAddresses().size(), deserBean.getAddresses().size());
        Assertions.assertEquals(bean.getAddresses().entrySet().iterator().next().getKey(),
                deserBean.getAddresses().entrySet().iterator().next().getKey());
        Assertions.assertEquals(bean.getAddresses().entrySet().iterator().next().getValue().getClass(),
                deserBean.getAddresses().entrySet().iterator().next().getValue().getClass());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSerializeJavaBeanDescriptor() {
        JavaBeanDescriptor descriptor = new JavaBeanDescriptor();
        JavaBeanDescriptor result = JavaBeanSerializeUtil.serialize(descriptor);
        Assertions.assertSame(descriptor, result);

        Map map = new HashMap();
        map.put("first", descriptor);
        result = JavaBeanSerializeUtil.serialize(map);
        Assertions.assertTrue(result.isMapType());
        Assertions.assertEquals(HashMap.class.getName(), result.getClassName());
        Assertions.assertEquals(map.size(), result.propertySize());
        Object object = result.iterator().next().getValue();
        Assertions.assertTrue(object instanceof JavaBeanDescriptor);
        JavaBeanDescriptor actual = (JavaBeanDescriptor) object;
        Assertions.assertEquals(map.get("first"), actual);
    }

    static void assertEqualsEnum(Enum<?> expected, Object obj) {
        JavaBeanDescriptor descriptor = (JavaBeanDescriptor) obj;
        Assertions.assertTrue(descriptor.isEnumType());
        Assertions.assertEquals(expected.getClass().getName(), descriptor.getClassName());
        Assertions.assertEquals(expected.name(), descriptor.getEnumPropertyName());
    }

    static void assertEqualsPrimitive(Object expected, Object obj) {
        if (expected == null) {
            return;
        }
        JavaBeanDescriptor descriptor = (JavaBeanDescriptor) obj;
        Assertions.assertTrue(descriptor.isPrimitiveType());
        Assertions.assertEquals(expected, descriptor.getPrimitiveProperty());
    }

    static void assertEqualsBigPerson(BigPerson person, Object obj) {
        JavaBeanDescriptor descriptor = (JavaBeanDescriptor) obj;
        Assertions.assertTrue(descriptor.isBeanType());
        assertEqualsPrimitive(person.getPersonId(), descriptor.getProperty("personId"));
        assertEqualsPrimitive(person.getLoginName(), descriptor.getProperty("loginName"));
        assertEqualsEnum(person.getStatus(), descriptor.getProperty("status"));
        assertEqualsPrimitive(person.getEmail(), descriptor.getProperty("email"));
        assertEqualsPrimitive(person.getPenName(), descriptor.getProperty("penName"));

        JavaBeanDescriptor infoProfile = (JavaBeanDescriptor) descriptor.getProperty("infoProfile");
        Assertions.assertTrue(infoProfile.isBeanType());
        JavaBeanDescriptor phones = (JavaBeanDescriptor) infoProfile.getProperty("phones");
        Assertions.assertTrue(phones.isCollectionType());
        assertEqualsPhone(person.getInfoProfile().getPhones().get(0), phones.getProperty(0));
        assertEqualsPhone(person.getInfoProfile().getPhones().get(1), phones.getProperty(1));
        assertEqualsPhone(person.getInfoProfile().getFax(), infoProfile.getProperty("fax"));
        assertEqualsFullAddress(person.getInfoProfile().getFullAddress(), infoProfile.getProperty("fullAddress"));
        assertEqualsPrimitive(person.getInfoProfile().getMobileNo(), infoProfile.getProperty("mobileNo"));
        assertEqualsPrimitive(person.getInfoProfile().getName(), infoProfile.getProperty("name"));
        assertEqualsPrimitive(person.getInfoProfile().getDepartment(), infoProfile.getProperty("department"));
        assertEqualsPrimitive(person.getInfoProfile().getJobTitle(), infoProfile.getProperty("jobTitle"));
        assertEqualsPrimitive(person.getInfoProfile().getHomepageUrl(), infoProfile.getProperty("homepageUrl"));
        assertEqualsPrimitive(person.getInfoProfile().isFemale(), infoProfile.getProperty("female"));
        assertEqualsPrimitive(person.getInfoProfile().isMale(), infoProfile.getProperty("male"));
    }

    static void assertEqualsPhone(Phone expected, Object obj) {
        JavaBeanDescriptor descriptor = (JavaBeanDescriptor) obj;
        Assertions.assertTrue(descriptor.isBeanType());
        if (expected.getArea() != null) {
            assertEqualsPrimitive(expected.getArea(), descriptor.getProperty("area"));
        }
        if (expected.getCountry() != null) {
            assertEqualsPrimitive(expected.getCountry(), descriptor.getProperty("country"));
        }
        if (expected.getExtensionNumber() != null) {
            assertEqualsPrimitive(expected.getExtensionNumber(), descriptor.getProperty("extensionNumber"));
        }
        if (expected.getNumber() != null) {
            assertEqualsPrimitive(expected.getNumber(), descriptor.getProperty("number"));
        }
    }

    static void assertEqualsFullAddress(FullAddress expected, Object obj) {
        JavaBeanDescriptor descriptor = (JavaBeanDescriptor) obj;
        Assertions.assertTrue(descriptor.isBeanType());
        if (expected.getCityId() != null) {
            assertEqualsPrimitive(expected.getCityId(), descriptor.getProperty("cityId"));
        }
        if (expected.getCityName() != null) {
            assertEqualsPrimitive(expected.getCityName(), descriptor.getProperty("cityName"));
        }
        if (expected.getCountryId() != null) {
            assertEqualsPrimitive(expected.getCountryId(), descriptor.getProperty("countryId"));
        }
        if (expected.getCountryName() != null) {
            assertEqualsPrimitive(expected.getCountryName(), descriptor.getProperty("countryName"));
        }
        if (expected.getProvinceName() != null) {
            assertEqualsPrimitive(expected.getProvinceName(), descriptor.getProperty("provinceName"));
        }
        if (expected.getStreetAddress() != null) {
            assertEqualsPrimitive(expected.getStreetAddress(), descriptor.getProperty("streetAddress"));
        }
        if (expected.getZipCode() != null) {
            assertEqualsPrimitive(expected.getZipCode(), descriptor.getProperty("zipCode"));
        }
    }

    static BigPerson createBigPerson() {
        BigPerson bigPerson;
        bigPerson = new BigPerson();
        bigPerson.setPersonId("superman111");
        bigPerson.setLoginName("superman");
        bigPerson.setStatus(PersonStatus.ENABLED);
        bigPerson.setEmail("sm@1.com");
        bigPerson.setPenName("pname");

        ArrayList<Phone> phones = new ArrayList<Phone>();
        Phone phone1 = new Phone("86", "0571", "87654321", "001");
        Phone phone2 = new Phone("86", "0571", "87654322", "002");
        phones.add(phone1);
        phones.add(phone2);

        PersonInfo pi = new PersonInfo();
        pi.setPhones(phones);
        Phone fax = new Phone("86", "0571", "87654321", null);
        pi.setFax(fax);
        FullAddress addr = new FullAddress("CN", "zj", "3480", "wensanlu", "315000");
        pi.setFullAddress(addr);
        pi.setMobileNo("13584652131");
        pi.setMale(true);
        pi.setDepartment("b2b");
        pi.setHomepageUrl("www.capcom.com");
        pi.setJobTitle("qa");
        pi.setName("superman");

        bigPerson.setInfoProfile(pi);
        return bigPerson;
    }
}
