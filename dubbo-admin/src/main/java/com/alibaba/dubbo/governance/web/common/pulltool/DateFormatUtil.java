
package com.alibaba.dubbo.governance.web.common.pulltool;
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
import org.apache.commons.lang.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * DateFormat Utility
 *
 */
public class DateFormatUtil {

    private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final ThreadLocal<Map<String, DateFormat>> tl = new ThreadLocal<Map<String, DateFormat>>();

    /**
     * According to the specified format, Get a DateFormat
     *
     * @param format
     * @return
     */
    public static DateFormat getDateFormat(String format) {
        Map<String, DateFormat> map = tl.get();

        if (map == null) {
            map = new HashMap<String, DateFormat>();
            tl.set(map);
        }

        if (StringUtils.isEmpty(format)) {
            format = DEFAULT_FORMAT;
        }

        DateFormat ret = map.get(format);

        if (ret == null) {
            ret = new SimpleDateFormat(format);
            map.put(format, ret);
        }

        return ret;
    }

    /**
     * Get Default DateFormat
     *
     * @return
     */
    public static DateFormat getDateFormat() {
        return getDateFormat(null);
    }
}
