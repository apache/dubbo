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

import org.apache.dubbo.common.utils.ReflectUtils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

interface Builder<T> {
    T getName(Bean bean);

    void setName(Bean bean, T name);
}

public class ClassGeneratorTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testMain() throws Exception {
        Bean b = new Bean();
        Field fname = null, fs[] = Bean.class.getDeclaredFields();
        for (Field f : fs) {
            ReflectUtils.makeAccessible(f);
            if (f.getName().equals("name"))
                fname = f;
        }

        ClassGenerator cg = ClassGenerator.newInstance();
        cg.setClassName(Bean.class.getName() + "$Builder");
        cg.addInterface(Builder.class);

        cg.addField("public static java.lang.reflect.Field FNAME;");

        cg.addMethod("public Object getName(" + Bean.class.getName() + " o){ boolean[][][] bs = new boolean[0][][]; return (String)FNAME.get($1); }");
        cg.addMethod("public void setName(" + Bean.class.getName() + " o, Object name){ FNAME.set($1, $2); }");

        cg.addDefaultConstructor();
        Class<?> cl = cg.toClass();
        cl.getField("FNAME").set(null, fname);

        System.out.println(cl.getName());
        Builder<String> builder = (Builder<String>) cl.newInstance();
        System.out.println(b.getName());
        builder.setName(b, "ok");
        System.out.println(b.getName());
    }

    @Test
    public void testMain0() throws Exception {
        Bean b = new Bean();
        Field fname = null, fs[] = Bean.class.getDeclaredFields();
        for (Field f : fs) {
            ReflectUtils.makeAccessible(f);
            if (f.getName().equals("name"))
                fname = f;
        }

        ClassGenerator cg = ClassGenerator.newInstance();
        cg.setClassName(Bean.class.getName() + "$Builder2");
        cg.addInterface(Builder.class);

        cg.addField("FNAME", Modifier.PUBLIC | Modifier.STATIC, java.lang.reflect.Field.class);

        cg.addMethod("public Object getName(" + Bean.class.getName() + " o){ boolean[][][] bs = new boolean[0][][]; return (String)FNAME.get($1); }");
        cg.addMethod("public void setName(" + Bean.class.getName() + " o, Object name){ FNAME.set($1, $2); }");

        cg.addDefaultConstructor();
        Class<?> cl = cg.toClass();
        cl.getField("FNAME").set(null, fname);

        System.out.println(cl.getName());
        Builder<String> builder = (Builder<String>) cl.newInstance();
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
