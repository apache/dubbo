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
package org.apache.dubbo.config;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.config.api.Greeting;
import org.apache.dubbo.config.support.Parameter;
import junit.framework.TestCase;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AbstractConfigTest {

    @Test
    public void testAppendProperties1() throws Exception {
        try {
            System.setProperty("dubbo.properties.i", "1");
            System.setProperty("dubbo.properties.c", "c");
            System.setProperty("dubbo.properties.b", "2");
            System.setProperty("dubbo.properties.d", "3");
            System.setProperty("dubbo.properties.f", "4");
            System.setProperty("dubbo.properties.l", "5");
            System.setProperty("dubbo.properties.s", "6");
            System.setProperty("dubbo.properties.str", "dubbo");
            System.setProperty("dubbo.properties.bool", "true");
            PropertiesConfig config = new PropertiesConfig();
            AbstractConfig.appendProperties(config);
            TestCase.assertEquals(1, config.getI());
            TestCase.assertEquals('c', config.getC());
            TestCase.assertEquals((byte) 0x02, config.getB());
            TestCase.assertEquals(3d, config.getD());
            TestCase.assertEquals(4f, config.getF());
            TestCase.assertEquals(5L, config.getL());
            TestCase.assertEquals(6, config.getS());
            TestCase.assertEquals("dubbo", config.getStr());
            TestCase.assertTrue(config.isBool());
        } finally {
            System.clearProperty("dubbo.properties.i");
            System.clearProperty("dubbo.properties.c");
            System.clearProperty("dubbo.properties.b");
            System.clearProperty("dubbo.properties.d");
            System.clearProperty("dubbo.properties.f");
            System.clearProperty("dubbo.properties.l");
            System.clearProperty("dubbo.properties.s");
            System.clearProperty("dubbo.properties.str");
            System.clearProperty("dubbo.properties.bool");
        }
    }

    @Test
    public void testAppendProperties2() throws Exception {
        try {
            System.setProperty("dubbo.properties.two.i", "2");
            PropertiesConfig config = new PropertiesConfig("two");
            AbstractConfig.appendProperties(config);
            TestCase.assertEquals(2, config.getI());
        } finally {
            System.clearProperty("dubbo.properties.two.i");
        }
    }

    @Test
    public void testAppendProperties3() throws Exception {
        try {
            Properties p = new Properties();
            p.put("dubbo.properties.str", "dubbo");
            ConfigUtils.setProperties(p);
            PropertiesConfig config = new PropertiesConfig();
            AbstractConfig.appendProperties(config);
            TestCase.assertEquals("dubbo", config.getStr());
        } finally {
            System.clearProperty(Constants.DUBBO_PROPERTIES_KEY);
            ConfigUtils.setProperties(null);
        }
    }

    @Test
    public void testAppendParameters1() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("default.num", "one");
        parameters.put("num", "ONE");
        AbstractConfig.appendParameters(parameters, new ParameterConfig(1, "hello/world", 30, "password"), "prefix");
        TestCase.assertEquals("one", parameters.get("prefix.key.1"));
        TestCase.assertEquals("two", parameters.get("prefix.key.2"));
        TestCase.assertEquals("ONE,one,1", parameters.get("prefix.num"));
        TestCase.assertEquals("hello%2Fworld", parameters.get("prefix.naming"));
        TestCase.assertEquals("30", parameters.get("prefix.age"));
        TestCase.assertFalse(parameters.containsKey("prefix.key-2"));
        TestCase.assertFalse(parameters.containsKey("prefix.secret"));
    }

    @Test(expected = IllegalStateException.class)
    public void testAppendParameters2() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractConfig.appendParameters(parameters, new ParameterConfig());
    }

    @Test
    public void testAppendParameters3() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractConfig.appendParameters(parameters, null);
        TestCase.assertTrue(parameters.isEmpty());
    }

    @Test
    public void testAppendParameters4() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        AbstractConfig.appendParameters(parameters, new ParameterConfig(1, "hello/world", 30, "password"));
        TestCase.assertEquals("one", parameters.get("key.1"));
        TestCase.assertEquals("two", parameters.get("key.2"));
        TestCase.assertEquals("1", parameters.get("num"));
        TestCase.assertEquals("hello%2Fworld", parameters.get("naming"));
        TestCase.assertEquals("30", parameters.get("age"));
    }

    @Test
    public void testAppendAttributes1() throws Exception {
        Map<Object, Object> parameters = new HashMap<Object, Object>();
        AbstractConfig.appendAttributes(parameters, new AttributeConfig('l', true, (byte) 0x01), "prefix");
        TestCase.assertEquals('l', parameters.get("prefix.let"));
        TestCase.assertEquals(true, parameters.get("prefix.activate"));
        TestCase.assertFalse(parameters.containsKey("prefix.flag"));
    }

    @Test
    public void testAppendAttributes2() throws Exception {
        Map<Object, Object> parameters = new HashMap<Object, Object>();
        AbstractConfig.appendAttributes(parameters, new AttributeConfig('l', true, (byte) 0x01));
        TestCase.assertEquals('l', parameters.get("let"));
        TestCase.assertEquals(true, parameters.get("activate"));
        TestCase.assertFalse(parameters.containsKey("flag"));
    }

    @Test(expected = IllegalStateException.class)
    public void checkExtension() throws Exception {
        AbstractConfig.checkExtension(Greeting.class, "hello", "world");
    }

    @Test(expected = IllegalStateException.class)
    public void checkMultiExtension1() throws Exception {
        AbstractConfig.checkMultiExtension(Greeting.class, "hello", "default,world");
    }

    @Test(expected = IllegalStateException.class)
    public void checkMultiExtension2() throws Exception {
        AbstractConfig.checkMultiExtension(Greeting.class, "hello", "default,-world");
    }

    @Test(expected = IllegalStateException.class)
    public void checkLength() throws Exception {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i <= 200; i++) {
            builder.append("a");
        }
        AbstractConfig.checkLength("hello", builder.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void checkPathLength() throws Exception {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i <= 200; i++) {
            builder.append("a");
        }
        AbstractConfig.checkPathLength("hello", builder.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void checkName() throws Exception {
        AbstractConfig.checkName("hello", "world%");
    }

    @Test
    public void checkNameHasSymbol() throws Exception {
        try {
            AbstractConfig.checkNameHasSymbol("hello", ":*,/-0123abcdABCD");
        } catch (Exception e) {
            TestCase.fail("the value should be legal.");
        }
    }

    @Test
    public void checkKey() throws Exception {
        try {
            AbstractConfig.checkKey("hello", "*,-0123abcdABCD");
        } catch (Exception e) {
            TestCase.fail("the value should be legal.");
        }
    }

    @Test
    public void checkMultiName() throws Exception {
        try {
            AbstractConfig.checkMultiName("hello", ",-._0123abcdABCD");
        } catch (Exception e) {
            TestCase.fail("the value should be legal.");
        }
    }

    @Test
    public void checkPathName() throws Exception {
        try {
            AbstractConfig.checkPathName("hello", "/-$._0123abcdABCD");
        } catch (Exception e) {
            TestCase.fail("the value should be legal.");
        }
    }

    @Test
    public void checkMethodName() throws Exception {
        try {
            AbstractConfig.checkMethodName("hello", "abcdABCD0123abcd");
        } catch (Exception e) {
            TestCase.fail("the value should be legal.");
        }

        try {
            AbstractConfig.checkMethodName("hello", "0a");
            TestCase.fail("the value should be illegal.");
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    public void checkParameterName() throws Exception {
        Map<String, String> parameters = Collections.singletonMap("hello", ":*,/-._0123abcdABCD");
        try {
            AbstractConfig.checkParameterName(parameters);
        } catch (Exception e) {
            TestCase.fail("the value should be legal.");
        }
    }

    @Test
    @Config(interfaceClass = Greeting.class, filter = {"f1, f2"}, listener = {"l1, l2"},
            parameters = {"k1", "v1", "k2", "v2"})
    public void appendAnnotation() throws Exception {
        Config config = getClass().getMethod("appendAnnotation").getAnnotation(Config.class);
        AnnotationConfig annotationConfig = new AnnotationConfig();
        annotationConfig.appendAnnotation(Config.class, config);
        TestCase.assertSame(Greeting.class, annotationConfig.getInterface());
        TestCase.assertEquals("f1, f2", annotationConfig.getFilter());
        TestCase.assertEquals("l1, l2", annotationConfig.getListener());
        TestCase.assertEquals(2, annotationConfig.getParameters().size());
        TestCase.assertEquals("v1", annotationConfig.getParameters().get("k1"));
        TestCase.assertEquals("v2", annotationConfig.getParameters().get("k2"));
        TestCase.assertEquals("<dubbo:annotation filter=\"f1, f2\" listener=\"l1, l2\" />",
                annotationConfig.toString());
    }

    private static class PropertiesConfig extends AbstractConfig {
        private char c;
        private boolean bool;
        private byte b;
        private int i;
        private long l;
        private float f;
        private double d;
        private short s;
        private String str;

        PropertiesConfig() {
        }

        PropertiesConfig(String id) {
            this.id = id;
        }

        public char getC() {
            return c;
        }

        public void setC(char c) {
            this.c = c;
        }

        public boolean isBool() {
            return bool;
        }

        public void setBool(boolean bool) {
            this.bool = bool;
        }

        public byte getB() {
            return b;
        }

        public void setB(byte b) {
            this.b = b;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public long getL() {
            return l;
        }

        public void setL(long l) {
            this.l = l;
        }

        public float getF() {
            return f;
        }

        public void setF(float f) {
            this.f = f;
        }

        public double getD() {
            return d;
        }

        public void setD(double d) {
            this.d = d;
        }

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public short getS() {
            return s;
        }

        public void setS(short s) {
            this.s = s;
        }
    }

    private static class ParameterConfig {
        private int number;
        private String name;
        private int age;
        private String secret;

        ParameterConfig() {
        }

        ParameterConfig(int number, String name, int age, String secret) {
            this.number = number;
            this.name = name;
            this.age = age;
            this.secret = secret;
        }

        @Parameter(key = "num", append = true)
        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        @Parameter(key = "naming", append = true, escaped = true, required = true)
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

        @Parameter(excluded = true)
        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Map getParameters() {
            Map<String, String> map = new HashMap<String, String>();
            map.put("key.1", "one");
            map.put("key-2", "two");
            return map;
        }
    }

    private static class AttributeConfig {
        private char letter;
        private boolean activate;
        private byte flag;

        public AttributeConfig(char letter, boolean activate, byte flag) {
            this.letter = letter;
            this.activate = activate;
            this.flag = flag;
        }

        @Parameter(attribute = true, key = "let")
        public char getLetter() {
            return letter;
        }

        public void setLetter(char letter) {
            this.letter = letter;
        }

        @Parameter(attribute = true)
        public boolean isActivate() {
            return activate;
        }

        public void setActivate(boolean activate) {
            this.activate = activate;
        }

        public byte getFlag() {
            return flag;
        }

        public void setFlag(byte flag) {
            this.flag = flag;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    public @interface Config {
        Class<?> interfaceClass() default void.class;

        String interfaceName() default "";

        String[] filter() default {};

        String[] listener() default {};

        String[] parameters() default {};
    }

    private static class AnnotationConfig extends AbstractConfig {
        private Class interfaceClass;
        private String filter;
        private String listener;
        private Map<String, String> parameters;

        public Class getInterface() {
            return interfaceClass;
        }

        public void setInterface(Class interfaceName) {
            this.interfaceClass = interfaceName;
        }

        public String getFilter() {
            return filter;
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

        public String getListener() {
            return listener;
        }

        public void setListener(String listener) {
            this.listener = listener;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
        }
    }
}
