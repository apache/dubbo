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

import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.config.api.Greeting;
import org.apache.dubbo.config.support.Parameter;

import junit.framework.TestCase;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertThat;

public class AbstractConfigTest {

    //FIXME
    /*@Test
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
    }*/

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
        Map<String, Object> parameters = new HashMap<String, Object>();
        AbstractConfig.appendAttributes(parameters, new AttributeConfig('l', true, (byte) 0x01), "prefix");
        TestCase.assertEquals('l', parameters.get("prefix.let"));
        TestCase.assertEquals(true, parameters.get("prefix.activate"));
        TestCase.assertFalse(parameters.containsKey("prefix.flag"));
    }

    @Test
    public void testAppendAttributes2() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
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
            AbstractConfig.checkNameHasSymbol("hello", ":*,/ -0123\tabcdABCD");
            AbstractConfig.checkNameHasSymbol("mock", "force:return world");
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
        assertThat(annotationConfig.toString(), Matchers.containsString("filter=\"f1, f2\" "));
        assertThat(annotationConfig.toString(), Matchers.containsString("listener=\"l1, l2\" "));
    }

    @Test
    public void testRefreshAll() {
        try {
            OverrideConfig overrideConfig = new OverrideConfig();
            overrideConfig.setAddress("override-config://127.0.0.1:2181");
            overrideConfig.setProtocol("override-config");
            overrideConfig.setEscape("override-config://");
            overrideConfig.setExclude("override-config");

            Map<String, String> external = new HashMap<>();
            external.put("dubbo.override.address", "external://127.0.0.1:2181");
            // @Parameter(exclude=true)
            external.put("dubbo.override.exclude", "external");
            // @Parameter(key="key1", useKeyAsProperty=false)
            external.put("dubbo.override.key", "external");
            // @Parameter(key="key2", useKeyAsProperty=true)
            external.put("dubbo.override.key2", "external");
            Environment.getInstance().setExternalConfigMap(external);

            System.setProperty("dubbo.override.address", "system://127.0.0.1:2181");
            System.setProperty("dubbo.override.protocol", "system");
            // this will not override, use 'key' instread, @Parameter(key="key1", useKeyAsProperty=false)
            System.setProperty("dubbo.override.key1", "system");
            System.setProperty("dubbo.override.key2", "system");

            // Load configuration from  system properties -> externalConfiguration -> RegistryConfig -> dubbo.properties
            overrideConfig.refresh();

            Assert.assertEquals("system://127.0.0.1:2181", overrideConfig.getAddress());
            Assert.assertEquals("system", overrideConfig.getProtocol());
            Assert.assertEquals("override-config://", overrideConfig.getEscape());
            Assert.assertEquals("external", overrideConfig.getKey());
            Assert.assertEquals("system", overrideConfig.getUseKeyAsProperty());
        } finally {
            System.clearProperty("dubbo.override.address");
            System.clearProperty("dubbo.override.protocol");
            System.clearProperty("dubbo.override.key1");
            System.clearProperty("dubbo.override.key2");
            Environment.getInstance().clearExternalConfigs();
        }
    }

    @Test
    public void testRefreshSystem() {
        try {
            OverrideConfig overrideConfig = new OverrideConfig();
            overrideConfig.setAddress("override-config://127.0.0.1:2181");
            overrideConfig.setProtocol("override-config");
            overrideConfig.setEscape("override-config://");
            overrideConfig.setExclude("override-config");

            System.setProperty("dubbo.override.address", "system://127.0.0.1:2181");
            System.setProperty("dubbo.override.protocol", "system");
            System.setProperty("dubbo.override.key", "system");

            overrideConfig.refresh();

            Assert.assertEquals("system://127.0.0.1:2181", overrideConfig.getAddress());
            Assert.assertEquals("system", overrideConfig.getProtocol());
            Assert.assertEquals("override-config://", overrideConfig.getEscape());
            Assert.assertEquals("system", overrideConfig.getKey());
        } finally {
            System.clearProperty("dubbo.override.address");
            System.clearProperty("dubbo.override.protocol");
            System.clearProperty("dubbo.override.key1");
            Environment.getInstance().clearExternalConfigs();
        }
    }

    @Test
    public void testRefreshProperties() {
        try {
            Environment.getInstance().setExternalConfigMap(new HashMap<>());
            OverrideConfig overrideConfig = new OverrideConfig();
            overrideConfig.setAddress("override-config://127.0.0.1:2181");
            overrideConfig.setProtocol("override-config");
            overrideConfig.setEscape("override-config://");

            overrideConfig.refresh();

            Assert.assertEquals("override-config://127.0.0.1:2181", overrideConfig.getAddress());
            Assert.assertEquals("override-config", overrideConfig.getProtocol());
            Assert.assertEquals("override-config://", overrideConfig.getEscape());
            Assert.assertEquals("properties", overrideConfig.getUseKeyAsProperty());
        } finally {
            Environment.getInstance().clearExternalConfigs();
        }
    }

    @Test
    public void testRefreshExternal() {
        try {
            OverrideConfig overrideConfig = new OverrideConfig();
            overrideConfig.setAddress("override-config://127.0.0.1:2181");
            overrideConfig.setProtocol("override-config");
            overrideConfig.setEscape("override-config://");
            overrideConfig.setExclude("override-config");

            Map<String, String> external = new HashMap<>();
            external.put("dubbo.override.address", "external://127.0.0.1:2181");
            external.put("dubbo.override.protocol", "external");
            external.put("dubbo.override.escape", "external://");
            // @Parameter(exclude=true)
            external.put("dubbo.override.exclude", "external");
            // @Parameter(key="key1", useKeyAsProperty=false)
            external.put("dubbo.override.key", "external");
            // @Parameter(key="key2", useKeyAsProperty=true)
            external.put("dubbo.override.key2", "external");
            Environment.getInstance().setExternalConfigMap(external);

            overrideConfig.refresh();

            Assert.assertEquals("external://127.0.0.1:2181", overrideConfig.getAddress());
            Assert.assertEquals("external", overrideConfig.getProtocol());
            Assert.assertEquals("external://", overrideConfig.getEscape());
            Assert.assertEquals("external", overrideConfig.getExclude());
            Assert.assertEquals("external", overrideConfig.getKey());
            Assert.assertEquals("external", overrideConfig.getUseKeyAsProperty());
        } finally {
            Environment.getInstance().clearExternalConfigs();
        }
    }

    @Test
    public void testRefreshId() {
        try {
            OverrideConfig overrideConfig = new OverrideConfig();
            overrideConfig.setId("override-id");
            overrideConfig.setAddress("override-config://127.0.0.1:2181");
            overrideConfig.setProtocol("override-config");
            overrideConfig.setEscape("override-config://");
            overrideConfig.setExclude("override-config");

            Map<String, String> external = new HashMap<>();
            external.put("dubbo.override.override-id.address", "external-override-id://127.0.0.1:2181");
            external.put("dubbo.override.address", "external://127.0.0.1:2181");
            // @Parameter(exclude=true)
            external.put("dubbo.override.exclude", "external");
            // @Parameter(key="key1", useKeyAsProperty=false)
            external.put("dubbo.override.key", "external");
            // @Parameter(key="key2", useKeyAsProperty=true)
            external.put("dubbo.override.key2", "external");
            Environment.getInstance().setExternalConfigMap(external);

            ConfigCenterConfig configCenter = new ConfigCenterConfig();
            configCenter.init();

            // Load configuration from  system properties -> externalConfiguration -> RegistryConfig -> dubbo.properties
            overrideConfig.refresh();

            Assert.assertEquals("external-override-id://127.0.0.1:2181", overrideConfig.getAddress());
            Assert.assertEquals("override-config", overrideConfig.getProtocol());
            Assert.assertEquals("override-config://", overrideConfig.getEscape());
            Assert.assertEquals("external", overrideConfig.getKey());
            Assert.assertEquals("external", overrideConfig.getUseKeyAsProperty());
        } finally {
            Environment.getInstance().clearExternalConfigs();
        }
    }

    @Test
    public void tetMetaData() {
        OverrideConfig overrideConfig = new OverrideConfig();
        overrideConfig.setId("override-id");
        overrideConfig.setAddress("override-config://127.0.0.1:2181");
        overrideConfig.setProtocol("override-config");
        overrideConfig.setEscape("override-config://");
        overrideConfig.setExclude("override-config");

        Map<String, String> metaData = overrideConfig.getMetaData();
        Assert.assertEquals("override-config://127.0.0.1:2181", metaData.get("address"));
        Assert.assertEquals("override-config", metaData.get("protocol"));
        Assert.assertEquals("override-config://", metaData.get("escape"));
        Assert.assertEquals("override-config", metaData.get("exclude"));
        Assert.assertNull(metaData.get("key"));
        Assert.assertNull(metaData.get("key2"));
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

    private static class OverrideConfig extends AbstractConfig {
        public String address;
        public String protocol;
        public String exclude;
        public String key;
        public String useKeyAsProperty;
        public String escape;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        @Parameter(excluded = true)
        public String getExclude() {
            return exclude;
        }

        public void setExclude(String exclude) {
            this.exclude = exclude;
        }

        @Parameter(key = "key1", useKeyAsProperty = false)
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @Parameter(key = "key2", useKeyAsProperty = true)
        public String getUseKeyAsProperty() {
            return useKeyAsProperty;
        }

        public void setUseKeyAsProperty(String useKeyAsProperty) {
            this.useKeyAsProperty = useKeyAsProperty;
        }

        @Parameter(escaped = true)
        public String getEscape() {
            return escape;
        }

        public void setEscape(String escape) {
            this.escape = escape;
        }
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
