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

package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import com.alibaba.com.caucho.hessian.io.SerializerFactory;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Calendar;

/**
 * Test Java8TimeSerializer class
 */
public class Java8TimeSerializerTest {

    private static SerializerFactory factory;
    private static ByteArrayOutputStream os;
    private static boolean isJava8;

    @BeforeClass
    public static void setUp() {
        String javaVersion = System.getProperty("java.specification.version");
        isJava8 = Double.valueOf(javaVersion) >= 1.8;
        factory = new SerializerFactory(Thread.currentThread().getContextClassLoader());
        os = new ByteArrayOutputStream();
    }

    @Test
    public void testNull() throws IOException {
        testJava8Time(null);
    }

    @Test
    public void testInstant() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.Instant");
            Method m = c.getDeclaredMethod("now");
            testJava8Time(m.invoke(null));
        }
    }

    @Test
    public void testDuration() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.Duration");
            Method m = c.getDeclaredMethod("ofDays", long.class);
            testJava8Time(m.invoke(null, 2));
        }
    }

    @Test
    public void testLocalDate() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.LocalDate");
            Method m = c.getDeclaredMethod("now");
            testJava8Time(m.invoke(null));
        }
    }

    @Test
    public void testLocalDateTime() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.LocalDateTime");
            Method m = c.getDeclaredMethod("now");
            testJava8Time(m.invoke(null));
        }
    }

    @Test
    public void testLocalTime() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.LocalTime");
            Method m = c.getDeclaredMethod("now");
            testJava8Time(m.invoke(null));
        }
    }

    @Test
    public void testYear() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.Year");
            Method m = c.getDeclaredMethod("now");
            testJava8Time(m.invoke(null));
        }
    }

    @Test
    public void testYearMonth() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.YearMonth");
            Method m = c.getDeclaredMethod("now");
            testJava8Time(m.invoke(null));
        }
    }

    @Test
    public void testMonthDay() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.MonthDay");
            Method m = c.getDeclaredMethod("now");
            testJava8Time(m.invoke(null));
        }
    }

    @Test
    public void testPeriod() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.Period");
            Method m = c.getDeclaredMethod("ofDays", int.class);
            testJava8Time(m.invoke(null, 3));
        }
    }

    @Test
    public void testOffsetTime() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.OffsetTime");
            Method m = c.getDeclaredMethod("now");
            testJava8Time(m.invoke(null));
        }
    }

    @Test
    public void testZoneOffset() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.ZoneOffset");
            Method m = c.getDeclaredMethod("ofHours", int.class);
            testJava8Time(m.invoke(null, 8));
        }
    }

    @Test
    public void testOffsetDateTime() throws Throwable {
        if (isJava8) {
            Class c = Class.forName("java.time.OffsetDateTime");
            Method m = c.getDeclaredMethod("now");
            testJava8Time(m.invoke(null));
        }
    }

    @Test
    public void testZonedDateTime() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.ZonedDateTime");
            Method m = c.getDeclaredMethod("now");
            testJava8Time(m.invoke(null));
        }
    }

    @Test
    public void testZoneId() throws Exception {
        if (isJava8) {
            Class c = Class.forName("java.time.ZoneId");
            Method m = c.getDeclaredMethod("of", String.class);
            testJava8Time(m.invoke(null, "America/New_York"));
        }
    }


    @Test
    public void testCalendar() throws IOException {
        Calendar calendar = Calendar.getInstance();
        testJava8Time(calendar);
    }

    private void testJava8Time(Object expected) throws IOException {
        os.reset();

        Hessian2Output output = new Hessian2Output(os);
        output.setSerializerFactory(factory);
        output.writeObject(expected);
        output.flush();

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        Hessian2Input input = new Hessian2Input(is);
        input.setSerializerFactory(factory);
        Object actual = input.readObject();

        TestCase.assertEquals(expected, actual);
    }
}
