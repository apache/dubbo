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
package org.apache.dubbo.common.utils;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateUtilsTest {

    @Test
    public void testFormatAndParse() {
        // Create a fixed date: 2026-01-18 10:00:00
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.JANUARY, 18, 10, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date date = calendar.getTime();

        // Test Formatting
        String formatted = DateUtils.format(date, DateUtils.DATE_TIME);
        Assertions.assertEquals("2026-01-18 10:00:00", formatted);

        // Test Parsing
        Date parsed = DateUtils.parse("2026-01-18 10:00:00", DateUtils.DATE_TIME);
        Assertions.assertEquals(date.getTime(), parsed.getTime());
    }

    @Test
    public void testParseObject() {
        // Test parsing a Long (Timestamp)
        Long timestamp = 1737230400000L;
        Date date = DateUtils.parse(timestamp);
        Assertions.assertNotNull(date);
        Assertions.assertEquals(timestamp, date.getTime());

        // Test parsing null
        Assertions.assertNull(DateUtils.parse((Object) null));
    }

    @Test
    public void testFormatGMT() {
        Date date = new Date(1737230400000L);
        String gmtResult = DateUtils.formatGMT(date, DateUtils.DATE_TIME_FORMAT);
        Assertions.assertNotNull(gmtResult);
        // GMT result should be consistent regardless of local machine timezone
    }
}
