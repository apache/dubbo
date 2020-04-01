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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Adaptive choose format pattern by date string
 */
public enum DatePattern {
    /**
     * BASIC_ISO_DATE
     * <p>
     * e.g. 20200101
     */
    BASIC_ISO_DATE("yyyyMMdd", "^\\d{8}$"),

    /**
     * ISO_DATE
     * e.g. 2020-01-01
     */
    ISO_DATE("yyyy-MM-dd", "^\\d{4}-\\d{2}-\\d{2}$"),

    /**
     * ISO_LOCAL_DATE_TIME
     * <p>
     * e.g. 2020-01-01T00:00:00
     */
    ISO_LOCAL_DATE_TIME("yyyy-MM-dd'T'HH:mm:ss", "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$"),

    /**
     * ISO_OFFSET_DATE_TIME
     * <p>
     * e.g. 2020-01-01T00:00:00+0800
     * 2020-01-01T00:00:00-0800
     */
    ISO_OFFSET_DATE_TIME("yyyy-MM-dd'T'HH:mm:ssZ", "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+|-]\\d{4}$"),

    /**
     * ISO_LOCAL_DATE_TIME_WITH_MINI
     * <p>
     * e.g. 2020-01-01T00:00:00.000
     */
    ISO_LOCAL_DATE_TIME_WITH_MINI("yyyy-MM-dd'T'HH:mm:ss.SSS", "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}$"),

    /**
     * ISO_OFFSET_DATE_TIME_WITH_MINI
     * <p>
     * e.g. 2020-01-01T00:00:00.000+0800
     * 2020-01-01T00:00:00.000-0800
     */
    ISO_OFFSET_DATE_TIME_WITH_MINI("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}[+|-]\\d{4}$"),

    /**
     * ISO_INSTANT (GMT)
     * <p>
     * e.g. 2020-01-01T00:00:00Z
     */
    ISO_INSTANT("yyyy-MM-dd'T'HH:mm:ss'Z'", "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$"),

    /**
     * DATE_TIME
     * <p>
     * e.g. 2020-01-01 00:00:00
     */
    DATE_TIME("yyyy-MM-dd HH:mm:ss", "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"),

    /**
     * DATE_TIME_WITH_MINI
     * <p>
     * e.g. 2020-01-01 00:00:00.000
     */
    DATE_TIME_WITH_MINI("yyyy-MM-dd HH:mm:ss.SSS", "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3}$"),

    /**
     * OFFSET_TIME
     * <p>
     * e.g. 2020-01-01 00:00:00+0800
     * 2020-01-01 00:00:00-0800
     */
    OFFSET_TIME("yyyy-MM-dd HH:mm:ssZ", "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}[+|-]\\d{2}:{0,1}\\d{2}$"),

    /**
     * OFFSET_TIME_WITH_MINI
     * <p>
     * e.g. 2020-01-01 00:00:00.000+0800
     * 2020-01-01 00:00:00.000-0800
     */
    OFFSET_TIME_WITH_MINI("yyyy-MM-dd HH:mm:ss.SSSZ", "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3}[+|-]\\d{4}$"),

    /**
     * YEAR_MONTH
     * <p>
     * e.g. 2020-01
     */
    YEAR_MONTH("yyyy-MM", "^\\d{4}-\\d{2}$");

    private String pattern;
    private Pattern regex;

    public String getPattern() {
        return pattern;
    }

    public Pattern getRegex() {
        return regex;
    }

    DatePattern(String pattern, String regex) {
        this.pattern = pattern;
        this.regex = Pattern.compile(regex);
    }

    public static Date parse(String dateStr) {
        DatePattern datePattern = null;
        for (DatePattern datePatternCheck : DatePattern.values()) {
            if (datePatternCheck.getRegex().matcher(dateStr).matches()) {
                datePattern = datePatternCheck;
                break;
            }
        }
        if (datePattern == null) {
            throw new IllegalStateException("Failed to match the format of \"" + dateStr + "\"");
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern.getPattern());
        if (datePattern.equals(DatePattern.ISO_INSTANT)) {
            // ISO_INSTANT is GMT time
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        try {
            return simpleDateFormat.parse(dateStr);
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse date " + dateStr + " by format "
                    + datePattern.getPattern() + ", cause: " + e.getMessage(), e);
        }
    }
}
