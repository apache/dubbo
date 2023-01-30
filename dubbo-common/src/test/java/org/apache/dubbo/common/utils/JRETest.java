package org.apache.dubbo.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


class JRETest {

    @Test
    void blankSystemVersion() {
        System.setProperty("java.version", "");
        JRE jre = JRE.currentVersion();
        Assertions.assertEquals(JRE.JAVA_8, jre);
    }

    @Test
    void java8Version() {
        JRE jre = JRE.currentVersion();
        Assertions.assertEquals(JRE.JAVA_8, jre);
    }

    @Test
    @Disabled
    void java19Version() {
        JRE jre = JRE.currentVersion();
        Assertions.assertNotEquals(JRE.JAVA_19, jre);
    }

}
