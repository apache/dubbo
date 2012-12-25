/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.common.beanutil;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.junit.Test;

import org.junit.Assert;

import com.alibaba.dubbo.common.model.person.BigPerson;
import com.alibaba.dubbo.common.model.person.FullAddress;
import com.alibaba.dubbo.common.model.person.PersonInfo;
import com.alibaba.dubbo.common.model.person.PersonStatus;
import com.alibaba.dubbo.common.model.person.Phone;
import com.alibaba.dubbo.common.utils.PojoUtilsTest;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class JavaBeanSerializeUtilTest {

    @Test
    public void testSerialize_Primitive() throws Exception {
        JavaBeanDescriptor descriptor;
        descriptor = JavaBeanSerializeUtil.serialize(Integer.MAX_VALUE);
        Assert.assertTrue(descriptor.isPrimitiveType());
        Assert.assertEquals(Integer.MAX_VALUE, descriptor.getProperty(JavaBeanDescriptor.PRIMITIVE_PROPERTY_VALUE));

        Date now = new Date();
        descriptor = JavaBeanSerializeUtil.serialize(now);
        Assert.assertTrue(descriptor.isPrimitiveType());
        Assert.assertEquals(now, descriptor.getProperty(JavaBeanDescriptor.PRIMITIVE_PROPERTY_VALUE));
    }

    @Test
    public void testDeserialize_Primitive() throws Exception {
        JavaBeanDescriptor descriptor = new JavaBeanDescriptor(long.class.getName(), JavaBeanDescriptor.TYPE_PRIMITIVE);
        descriptor.setProperty(JavaBeanDescriptor.PRIMITIVE_PROPERTY_VALUE, Long.MAX_VALUE);
        Assert.assertEquals(Long.MAX_VALUE, JavaBeanSerializeUtil.deserialize(descriptor));

        BigDecimal decimal = BigDecimal.TEN;
        Assert.assertEquals(Long.MAX_VALUE, descriptor.setProperty(JavaBeanDescriptor.PRIMITIVE_PROPERTY_VALUE, decimal));
        Assert.assertEquals(decimal, JavaBeanSerializeUtil.deserialize(descriptor));

        String string = UUID.randomUUID().toString();
        Assert.assertEquals(decimal, descriptor.setProperty(JavaBeanDescriptor.PRIMITIVE_PROPERTY_VALUE, string));
        Assert.assertEquals(string, JavaBeanSerializeUtil.deserialize(descriptor));
    }

    @Test
    public void testSerialize_Array() throws Exception {
        int[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        JavaBeanDescriptor descriptor = JavaBeanSerializeUtil.serialize(array, JavaBeanAccessor.METHOD);
        Assert.assertTrue(descriptor.isArrayType());
        Assert.assertEquals(int.class.getName(), descriptor.getClassName());
        for (int i = 0; i < array.length; i++) {
            Assert.assertEquals(array[i],
                                ((JavaBeanDescriptor) descriptor.getProperty(i)).getProperty(JavaBeanDescriptor.PRIMITIVE_PROPERTY_VALUE));
        }

        int[][] second = {{1, 2}, {3, 4}};
        descriptor = JavaBeanSerializeUtil.serialize(second, JavaBeanAccessor.METHOD);
        Assert.assertTrue(descriptor.isArrayType());
        Assert.assertEquals(int[].class.getName(), descriptor.getClassName());
        for(int i = 0; i < second.length; i++) {
            for(int j = 0; j < second[i].length; j++) {
                JavaBeanDescriptor item = (((JavaBeanDescriptor)descriptor.getProperty(i)));
                Assert.assertTrue(item.isArrayType());
                Assert.assertEquals(int.class.getName(), item.getClassName());
                Assert.assertEquals(second[i][j], ((JavaBeanDescriptor)item.getProperty(j)).getPrimitive());
            }
        }

        BigPerson[] persons = new BigPerson[] {createBigPerson(), createBigPerson()};
        descriptor = JavaBeanSerializeUtil.serialize(persons);
        Assert.assertTrue(descriptor.isArrayType());
        Assert.assertEquals(BigPerson.class.getName(), descriptor.getClassName());
        for(int i = 0; i < persons.length; i++) {
            assertEqualsBigPerson(persons[i], descriptor.getProperty(i));
        }
    }

    @Test
    public void testDeserialize_Array() throws Exception {
        final int len = 10;
        JavaBeanDescriptor descriptor = new JavaBeanDescriptor(int.class.getName(), JavaBeanDescriptor.TYPE_ARRAY);
        for(int i = 0; i < len; i++) {
            descriptor.setProperty(i, i);
        }

        Object obj = JavaBeanSerializeUtil.deserialize(descriptor);
        Assert.assertTrue(obj.getClass().isArray());
        Assert.assertTrue(int.class == obj.getClass().getComponentType());
        for(int i = 0; i < len; i++) {
            Assert.assertEquals(i, Array.get(obj, i));
        }

        descriptor = new JavaBeanDescriptor(int[].class.getName(), JavaBeanDescriptor.TYPE_ARRAY);
        for(int i = 0; i < len; i++) {
            JavaBeanDescriptor innerItem = new JavaBeanDescriptor(int.class.getName(), JavaBeanDescriptor.TYPE_ARRAY);
            for (int j = 0; j < len; j++) {
                innerItem.setProperty(j, j);
            }
            descriptor.setProperty(i, innerItem);
        }
        obj = JavaBeanSerializeUtil.deserialize(descriptor);
        Assert.assertTrue(obj.getClass().isArray());
        Assert.assertEquals(int[].class, obj.getClass().getComponentType());
        for(int i = 0; i < len; i++) {
            Object innerItem = Array.get(obj, i);
            Assert.assertTrue(innerItem.getClass().isArray());
            Assert.assertEquals(int.class, innerItem.getClass().getComponentType());
            for (int j = 0; j < len; j++) {
                Assert.assertEquals(j, Array.get(innerItem, j));
            }
        }

        descriptor = new JavaBeanDescriptor(BigPerson[].class.getName(), JavaBeanDescriptor.TYPE_ARRAY);
        JavaBeanDescriptor innerDescriptor = new JavaBeanDescriptor(BigPerson.class.getName(), JavaBeanDescriptor.TYPE_ARRAY);
        innerDescriptor.setProperty(0, JavaBeanSerializeUtil.serialize(createBigPerson(), JavaBeanAccessor.METHOD));
        descriptor.setProperty(0, innerDescriptor);

        obj = JavaBeanSerializeUtil.deserialize(descriptor);
        Assert.assertTrue(obj.getClass().isArray());
        Assert.assertEquals(BigPerson[].class, obj.getClass().getComponentType());
        Assert.assertEquals(1, Array.getLength(obj));
        obj = Array.get(obj, 0);
        Assert.assertTrue(obj.getClass().isArray());
        Assert.assertEquals(BigPerson.class, obj.getClass().getComponentType());
        Assert.assertEquals(1, Array.getLength(obj));
        Assert.assertEquals(createBigPerson(), Array.get(obj, 0));
    }

    @Test
    public void test_Circular_Reference() throws Exception {
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
        Assert.assertTrue(descriptor.isBeanType());
        assertEqualsPrimitive(parent.getAge(), descriptor.getProperty("age"));
        assertEqualsPrimitive(parent.getName(), descriptor.getProperty("name"));
        assertEqualsPrimitive(parent.getEmail(), descriptor.getProperty("email"));

        JavaBeanDescriptor childDescriptor = (JavaBeanDescriptor) descriptor.getProperty("child");
        Assert.assertTrue(descriptor == childDescriptor.getProperty("parent"));
        assertEqualsPrimitive(child.getName(), childDescriptor.getProperty("name"));
        assertEqualsPrimitive(child.getAge(), childDescriptor.getProperty("age"));
    }

    static void assertEqualsEnum(Enum<?> expected, Object obj) {
        JavaBeanDescriptor descriptor = (JavaBeanDescriptor) obj;
        Assert.assertTrue(descriptor.isEnumType());
        Assert.assertEquals(expected.getClass().getName(), descriptor.getClassName());
        Assert.assertEquals(expected.name(), descriptor.getProperty(JavaBeanDescriptor.ENUM_PROPERTY_NAME));
    }

    static void assertEqualsPrimitive(Object expected, Object obj) {
        if (expected == null) { return; }
        JavaBeanDescriptor descriptor = (JavaBeanDescriptor) obj;
        Assert.assertTrue(descriptor.isPrimitiveType());
        Assert.assertEquals(expected, descriptor.getPrimitive());
    }

    static void assertEqualsBigPerson(BigPerson person, Object obj) {
        JavaBeanDescriptor descriptor = (JavaBeanDescriptor)obj;
        Assert.assertTrue(descriptor.isBeanType());
        assertEqualsPrimitive(person.getPersonId(), descriptor.getProperty("personId"));
        assertEqualsPrimitive(person.getLoginName(), descriptor.getProperty("loginName"));
        assertEqualsEnum(person.getStatus(), descriptor.getProperty("status"));
        assertEqualsPrimitive(person.getEmail(), descriptor.getProperty("email"));
        assertEqualsPrimitive(person.getPenName(), descriptor.getProperty("penName"));

        JavaBeanDescriptor infoProfile = (JavaBeanDescriptor)descriptor.getProperty("infoProfile");
        Assert.assertTrue(infoProfile.isBeanType());
        JavaBeanDescriptor phones = (JavaBeanDescriptor) infoProfile.getProperty("phones");
        Assert.assertTrue(phones.isCollectionType());
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

    static void assertEqualsPhone(Phone excpected, Object obj) {
        JavaBeanDescriptor descriptor = (JavaBeanDescriptor)obj;
        Assert.assertTrue(descriptor.isBeanType());
        if (excpected.getArea() != null) {
            assertEqualsPrimitive(excpected.getArea(), descriptor.getProperty("area"));
        }
        if (excpected.getCountry() != null) {
            assertEqualsPrimitive(excpected.getCountry(), descriptor.getProperty("country"));
        }
        if (excpected.getExtensionNumber()!= null) {
            assertEqualsPrimitive(excpected.getExtensionNumber(), descriptor.getProperty("extensionNumber"));
        }
        if (excpected.getNumber() != null) {
            assertEqualsPrimitive(excpected.getNumber(), descriptor.getProperty("number"));
        }
    }

    static void assertEqualsFullAddress(FullAddress expected, Object obj) {
        JavaBeanDescriptor descriptor = (JavaBeanDescriptor)obj;
        Assert.assertTrue(descriptor.isBeanType());
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
