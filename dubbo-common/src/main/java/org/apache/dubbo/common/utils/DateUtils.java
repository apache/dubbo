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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DateUtils {

    public static final ZoneId GMT = ZoneId.of("GMT");
    public static final ZoneId UTC = ZoneId.of("UTC");

    public static final String DATE = "yyyy-MM-dd";
    public static final String DATE_MIN = "yyyy-MM-dd HH:mm";
    public static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    public static final String JDK_TIME = "EEE MMM dd HH:mm:ss zzz yyyy";
    public static final String ASC_TIME = "EEE MMM d HH:mm:ss yyyy";
    public static final String RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(DATE);
    public static final DateTimeFormatter DATE_MIN_FORMAT = DateTimeFormatter.ofPattern(DATE_MIN);
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern(DATE_TIME);
    public static final DateTimeFormatter JDK_TIME_FORMAT = DateTimeFormatter.ofPattern(JDK_TIME, Locale.US);
    public static final DateTimeFormatter ASC_TIME_FORMAT = DateTimeFormatter.ofPattern(ASC_TIME, Locale.US);
    public static final DateTimeFormatter RFC1036_FORMAT = DateTimeFormatter.ofPattern(RFC1036, Locale.US);

    private static final Map<String, DateTimeFormatter> CACHE = new LRUCache<>(64);
    private static final List<DateTimeFormatter> CUSTOM_FORMATTERS = new CopyOnWriteArrayList<>();

    private DateUtils() {}

    public static void registerFormatter(String pattern) {
        CUSTOM_FORMATTERS.add(DateTimeFormatter.ofPattern(pattern));
    }

    public static void registerFormatter(DateTimeFormatter formatter) {
        CUSTOM_FORMATTERS.add(formatter);
    }

    public static Date parse(String str, String pattern) {
        if (DATE_TIME.equals(pattern)) {
            return parse(str, DATE_TIME_FORMAT);
        }
        DateTimeFormatter formatter = getFormatter(pattern);
        return parse(str, formatter);
    }

    public static Date parse(String str, DateTimeFormatter formatter) {
        return toDate(formatter.parse(str));
    }

    public static String format(Date date) {
        return format(date, DATE_TIME_FORMAT);
    }

    public static String format(Date date, String pattern) {
        if (DATE_TIME.equals(pattern)) {
            return format(date, DATE_TIME_FORMAT);
        }
        DateTimeFormatter formatter = getFormatter(pattern);
        return format(date, formatter);
    }

    public static String format(Date date, DateTimeFormatter formatter) {
        return formatter.format(ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }

    public static String format(Date date, DateTimeFormatter formatter, ZoneId zone) {
        return formatter.format(ZonedDateTime.ofInstant(date.toInstant(), zone));
    }

    public static String formatGMT(Date date, DateTimeFormatter formatter) {
        return formatter.format(ZonedDateTime.ofInstant(date.toInstant(), GMT));
    }

    public static String formatUTC(Date date, DateTimeFormatter formatter) {
        return formatter.format(ZonedDateTime.ofInstant(date.toInstant(), UTC));
    }

    public static String formatHeader(Date date) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(date.toInstant(), GMT));
    }

    private static DateTimeFormatter getFormatter(String pattern) {
        return CACHE.computeIfAbsent(pattern, DateTimeFormatter::ofPattern);
    }

    public static Date parse(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }
        if (value.getClass() == Instant.class) {
            return Date.from((Instant) value);
        }
        if (value instanceof TemporalAccessor) {
            return Date.from(Instant.from((TemporalAccessor) value));
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        if (value instanceof CharSequence) {
            return parse(value.toString());
        }
        throw new IllegalArgumentException("Can not cast to Date, value : '" + value + "'");
    }

    public static Date parse(String value) {
        if (value == null) {
            return null;
        }
        String str = value.trim();
        int len = str.length();
        if (len == 0) {
            return null;
        }

        boolean isIso = true;
        boolean isNumeric = true;
        boolean hasDate = false;
        boolean hasTime = false;
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            switch (c) {
                case ' ':
                    isIso = false;
                    break;
                case '-':
                    hasDate = true;
                    break;
                case 'T':
                case ':':
                    hasTime = true;
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    continue;
                default:
            }
            if (isNumeric) {
                isNumeric = false;
            }
        }
        DateTimeFormatter formatter = null;
        if (isIso) {
            if (hasDate) {
                formatter = hasTime ? DateTimeFormatter.ISO_DATE_TIME : DateTimeFormatter.ISO_DATE;
            } else if (hasTime) {
                formatter = DateTimeFormatter.ISO_TIME;
            }
        }
        if (isNumeric) {
            long num = Long.parseLong(str);
            if (num > 21000101 || num < 19700101) {
                return new Date(num);
            }
            formatter = DateTimeFormatter.BASIC_ISO_DATE;
        }
        switch (len) {
            case 10:
                formatter = DATE_FORMAT;
                break;
            case 16:
                formatter = DATE_MIN_FORMAT;
                break;
            case 19:
                formatter = DATE_TIME_FORMAT;
                break;
            case 23:
            case 24:
                formatter = ASC_TIME_FORMAT;
                break;
            case 27:
                formatter = RFC1036_FORMAT;
                break;
            case 28:
                formatter = JDK_TIME_FORMAT;
                break;
            case 29:
                formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
                break;
            default:
        }

        if (formatter != null) {
            try {
                return toDate(formatter.parse(str));
            } catch (Exception ignored) {
            }
        }
        for (DateTimeFormatter dtf : CUSTOM_FORMATTERS) {
            try {
                return parse(str, dtf);
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException("Can not cast to Date, value : '" + value + "'");
    }

    public static Date toDate(TemporalAccessor temporal) {
        if (temporal instanceof Instant) {
            return Date.from((Instant) temporal);
        }
        long timestamp;
        if (temporal.isSupported(ChronoField.EPOCH_DAY)) {
            timestamp = temporal.getLong(ChronoField.EPOCH_DAY) * 86400000;
        } else {
            timestamp = LocalDate.now().toEpochDay() * 86400000;
        }
        if (temporal.isSupported(ChronoField.MILLI_OF_DAY)) {
            timestamp += temporal.getLong(ChronoField.MILLI_OF_DAY);
        }
        if (temporal.isSupported(ChronoField.OFFSET_SECONDS)) {
            timestamp -= temporal.getLong(ChronoField.OFFSET_SECONDS) * 1000;
        } else {
            timestamp -= TimeZone.getDefault().getRawOffset();
        }
        return new Date(timestamp);
    }
}
