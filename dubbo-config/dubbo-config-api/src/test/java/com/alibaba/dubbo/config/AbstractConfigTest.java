package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.config.support.Parameter;
import junit.framework.TestCase;
import org.junit.Test;

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
            ConfigUtils.setProperties(new Properties());
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
}
