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
package org.apache.dubbo.common.bytecode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

interface Builder<T> {
    T getName(Bean bean);

    void setName(Bean bean, T name);
}

class BaseClass {

    public BaseClass() {

    }

    public BaseClass(StringBuilder sb) {
        sb.append("constructor comes from BaseClass");
    }

    public String baseClassMethod() {
        return "method comes from BaseClass";
    }
}

interface BaseInterface {

}
class ClassGeneratorTest {

    @Test
    void test() throws Exception {
        ClassGenerator cg = ClassGenerator.newInstance();

        // add className, interface, superClass
        String className = BaseClass.class.getPackage().getName() + ".TestClass";
        cg.setClassName(className);
        cg.addInterface(BaseInterface.class);
        cg.setSuperClass(BaseClass.class);

        // add constructor
        cg.addDefaultConstructor();
        cg.addConstructor(Modifier.PUBLIC, new Class[]{String.class, int.class}, new Class[]{Throwable.class, RuntimeException.class}, "this.strAttr = arg0;this.intAttr = arg1;");
        cg.addConstructor(BaseClass.class.getConstructor(StringBuilder.class));

        // add field
        cg.addField("staticAttr", Modifier.PUBLIC | Modifier.STATIC | Modifier.VOLATILE, String.class, "\"defaultVal\"");
        cg.addField("strAttr", Modifier.PROTECTED | Modifier.VOLATILE, String.class);
        cg.addField("intAttr", Modifier.PRIVATE | Modifier.VOLATILE, int.class);

        // add method
        cg.addMethod("setStrAttr", Modifier.PUBLIC, void.class, new Class[]{String.class, int.class}, new Class[]{Throwable.class, RuntimeException.class}, "this.strAttr = arg0;");
        cg.addMethod("setIntAttr", Modifier.PUBLIC, void.class, new Class[]{String.class, int.class}, new Class[]{Throwable.class, RuntimeException.class}, "this.intAttr = arg1;");
        cg.addMethod("getStrAttr", Modifier.PUBLIC, String.class, new Class[]{}, new Class[]{Throwable.class, RuntimeException.class}, "return this.strAttr;");
        cg.addMethod("getIntAttr", Modifier.PUBLIC, int.class, new Class[]{}, new Class[]{Throwable.class, RuntimeException.class}, "return this.intAttr;");
        cg.addMethod(BaseClass.class.getMethod("baseClassMethod"));

        // cg.toClass
        Class<?> clz = cg.toClass(Bean.class);
        // after cg.toClass, the TestClass.class generated by javassist is like the following file content
        getClass().getResource("/org/apache/dubbo/common/bytecode/TestClass");

        // verify class, superClass, interfaces
        Assertions.assertTrue(ClassGenerator.isDynamicClass(clz));
        Assertions.assertEquals(clz.getName(), className);
        Assertions.assertEquals(clz.getSuperclass(), BaseClass.class);
        Assertions.assertArrayEquals(clz.getInterfaces(), new Class[]{ClassGenerator.DC.class, BaseInterface.class});

        // get constructors
        Constructor<?>[] constructors = clz.getConstructors();
        Assertions.assertEquals(constructors.length, 3);
        Constructor<?> constructor0 = clz.getConstructor();
        Constructor<?> constructor1 = clz.getConstructor(new Class[]{String.class, int.class});
        Constructor<?> constructor2 = clz.getConstructor(new Class[]{StringBuilder.class});
        Assertions.assertEquals(constructor1.getModifiers(), Modifier.PUBLIC);
        Assertions.assertArrayEquals(constructor1.getExceptionTypes(), new Class[]{Throwable.class, RuntimeException.class});

        // get fields
        Field staticAttrField = clz.getDeclaredField("staticAttr");
        Field strAttrField = clz.getDeclaredField("strAttr");
        Field intAttrField = clz.getDeclaredField("intAttr");
        Assertions.assertNotNull(staticAttrField);
        Assertions.assertNotNull(strAttrField);
        Assertions.assertNotNull(intAttrField);
        Assertions.assertEquals(staticAttrField.get(null), "defaultVal");

        // get methods
        Method setStrAttrMethod = clz.getMethod("setStrAttr", new Class[]{String.class, int.class});
        Method setIntAttrMethod = clz.getMethod("setIntAttr", new Class[]{String.class, int.class});
        Method getStrAttrMethod = clz.getMethod("getStrAttr");
        Method getIntAttrMethod = clz.getMethod("getIntAttr");
        Method baseClassMethod = clz.getMethod("baseClassMethod");
        Assertions.assertNotNull(setStrAttrMethod);
        Assertions.assertNotNull(setIntAttrMethod);
        Assertions.assertNotNull(getStrAttrMethod);
        Assertions.assertNotNull(getIntAttrMethod);
        Assertions.assertNotNull(baseClassMethod);

        // verify constructor0
        Object objByConstructor0 = constructor0.newInstance();
        Assertions.assertEquals(getStrAttrMethod.invoke(objByConstructor0), null);
        Assertions.assertEquals(getIntAttrMethod.invoke(objByConstructor0), 0);

        // verify constructor1
        Object objByConstructor1 = constructor1.newInstance("v1", 1);
        Assertions.assertEquals(getStrAttrMethod.invoke(objByConstructor1), "v1");
        Assertions.assertEquals(getIntAttrMethod.invoke(objByConstructor1), 1);

        // verify getter setter method
        setStrAttrMethod.invoke(objByConstructor0, "v2", 2);
        setIntAttrMethod.invoke(objByConstructor0, "v3", 3);
        Assertions.assertEquals(getStrAttrMethod.invoke(objByConstructor0), "v2");
        Assertions.assertEquals(getIntAttrMethod.invoke(objByConstructor0), 3);

        // verify constructor and method witch comes from baseClass
        StringBuilder sb = new StringBuilder();
        Object objByConstructor2 = constructor2.newInstance(sb);
        Assertions.assertEquals(sb.toString(),"constructor comes from BaseClass");

        Object res = baseClassMethod.invoke(objByConstructor2);
        Assertions.assertEquals(res,"method comes from BaseClass");

        cg.release();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testMain() throws Exception {
        Bean b = new Bean();
        Field fname = Bean.class.getDeclaredField("name");
        fname.setAccessible(true);

        ClassGenerator cg = ClassGenerator.newInstance();
        cg.setClassName(Bean.class.getName() + "$Builder");
        cg.addInterface(Builder.class);

        cg.addField("public static java.lang.reflect.Field FNAME;");

        cg.addMethod("public Object getName(" + Bean.class.getName() + " o){ boolean[][][] bs = new boolean[0][][]; return (String)FNAME.get($1); }");
        cg.addMethod("public void setName(" + Bean.class.getName() + " o, Object name){ FNAME.set($1, $2); }");

        cg.addDefaultConstructor();
        Class<?> cl = cg.toClass(Bean.class);
        cl.getField("FNAME").set(null, fname);

        System.out.println(cl.getName());
        Builder<String> builder = (Builder<String>) cl.getDeclaredConstructor().newInstance();
        System.out.println(b.getName());
        builder.setName(b, "ok");
        System.out.println(b.getName());
    }

    @Test
    void testMain0() throws Exception {
        Bean b = new Bean();
        Field fname = Bean.class.getDeclaredField("name");
        fname.setAccessible(true);

        ClassGenerator cg = ClassGenerator.newInstance();
        cg.setClassName(Bean.class.getName() + "$Builder2");
        cg.addInterface(Builder.class);

        cg.addField("FNAME", Modifier.PUBLIC | Modifier.STATIC, java.lang.reflect.Field.class);

        cg.addMethod("public Object getName(" + Bean.class.getName() + " o){ boolean[][][] bs = new boolean[0][][]; return (String)FNAME.get($1); }");
        cg.addMethod("public void setName(" + Bean.class.getName() + " o, Object name){ FNAME.set($1, $2); }");

        cg.addDefaultConstructor();
        Class<?> cl = cg.toClass(Bean.class);
        cl.getField("FNAME").set(null, fname);

        System.out.println(cl.getName());
        Builder<String> builder = (Builder<String>) cl.getDeclaredConstructor().newInstance();
        System.out.println(b.getName());
        builder.setName(b, "ok");
        System.out.println(b.getName());
    }
}

class Bean {
    int age = 30;

    private String name = "qianlei";

    public int getAge() {
        return age;
    }

    public String getName() {
        return name;
    }

    public static volatile String abc = "df";
}
