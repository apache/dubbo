package com.alibaba.com.caucho.hessian.io.java8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import com.alibaba.com.caucho.hessian.io.SerializerFactory;

/**
 *  jdk1.8时间类型的序列化测试
 */
public class Java8TimeSerializerTest {

    private static SerializerFactory factory;
    private static ByteArrayOutputStream os;

    @BeforeClass
    public static void setUp() {
        factory = new SerializerFactory(Thread.currentThread().getContextClassLoader());
        os = new ByteArrayOutputStream();
    }

    @Test
    public void testNull() throws IOException {
        testJava8Time(null);
    }

    @Test
    public void testInstant() throws IOException {
        testJava8Time(Instant.now());
    }

    @Test
    public void testDuration() throws IOException {
        testJava8Time(Duration.ofDays(2));
    }

    @Test
    public void testLocalDate() throws IOException {
        testJava8Time(LocalDate.now());
    }

    @Test
    public void testLocalDateTime() throws IOException {
        testJava8Time(LocalDateTime.now());
    }
    
    @Test
    public void testLocalTime() throws IOException {
        testJava8Time(LocalTime.now());
    }

    @Test
    public void testYear() throws IOException {
        testJava8Time(Year.now());
    }

    @Test
    public void testYearMonth() throws IOException {
        testJava8Time(YearMonth.now());
    }

    @Test
    public void testMonthDay() throws IOException {
        testJava8Time(MonthDay.now());
    }

    @Test
    public void testPeriod() throws IOException {
        testJava8Time(Period.ofDays(3));
    }

    @Test
    public void testOffsetTime() throws IOException {
        testJava8Time(OffsetTime.now());
    }

    @Test
    public void testZoneOffset() throws IOException {
        testJava8Time(ZoneOffset.ofHours(8));
    }

    @Test
    public void testOffsetDateTime() throws IOException {
        testJava8Time(OffsetDateTime.now());
    }

    @Test
    public void testZonedDateTime() throws IOException {
        testJava8Time(ZonedDateTime.now());
    }
    
    @Test
    public void testZoneId() throws IOException {
        testJava8Time(ZoneId.of("America/New_York"));
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

        Assert.assertEquals(expected, actual);
    }
}
