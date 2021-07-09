package org.apache.dubbo.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RegexPropertiesTest {
    @Test
    public void testGetProperty(){
        RegexProperties regexProperties = new RegexProperties();
        regexProperties.setProperty("org.apache.*", "http://localhost:20880");
        regexProperties.setProperty("org.apache.dubbo.*", "http://localhost:30880");
        regexProperties.setProperty("org.apache.dubbo.demo", "http://localhost:40880");

        Assertions.assertEquals("http://localhost:40880", regexProperties.getProperty("org.apache.dubbo.demo"));
        Assertions.assertEquals("http://localhost:30880", regexProperties.getProperty("org.apache.dubbo.provider"));
        Assertions.assertEquals("http://localhost:20880", regexProperties.getProperty("org.apache.demo"));
    }
}
