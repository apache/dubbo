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
package com.alibaba.dubbo.registry.common.util;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;

import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Tool
 *
 */
public class Tool {

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
    private static final Comparator<String> SIMPLE_NAME_COMPARATOR = new Comparator<String>() {
        public int compare(String s1, String s2) {
            if (s1 == null && s2 == null) {
                return 0;
            }
            if (s1 == null) {
                return -1;
            }
            if (s2 == null) {
                return 1;
            }
            s1 = getSimpleName(s1);
            s2 = getSimpleName(s2);
            return s1.compareToIgnoreCase(s2);
        }
    };

    public static boolean startWith(String value, String prefix) {
        return value.startsWith(prefix);
    }

    public static boolean isContains(String[] values, String value) {
        return StringUtils.isContains(values, value);
    }

    public static boolean isValidAddress(String address) {
        return IP_PATTERN.matcher(address).matches();
    }

    public static String getHostName(String address) {
        if (address != null && address.length() > 0) {
            String hostname = NetUtils.getHostName(address);
            if (!address.equals(hostname)) {
                return hostname + "/";
            }
        }
        return "";
    }

    public static String getIP(String address) {
        if (address != null && address.length() > 0) {
            int i = address.indexOf("://");
            if (i >= 0) {
                address = address.substring(i + 3);
            }
            i = address.indexOf('/');
            if (i >= 0) {
                address = address.substring(0, i);
            }
            i = address.indexOf('@');
            if (i >= 0) {
                address = address.substring(i + 1);
            }
            i = address.indexOf(':');
            if (i >= 0) {
                address = address.substring(0, i);
            }
            if (address.matches("[a-zA-Z]+")) {
                try {
                    address = InetAddress.getByName(address).getHostAddress();
                } catch (UnknownHostException e) {
                }
            }
        }
        return address;
    }

    public static String encodeUrl(String url) {
        return URL.encode(url);
    }

    public static String decodeUrl(String url) {
        return URL.decode(url);
    }

    public static String encodeHtml(String html) {
        return StringEscapeUtils.escapeHtml(html);
    }

    public static String decodeHtml(String html) {
        return StringEscapeUtils.unescapeHtml(html);
    }

    public static int countMapValues(Map<?, ?> map) {
        int total = 0;
        if (map != null && map.size() > 0) {
            for (Object value : map.values()) {
                if (value != null) {
                    if (value instanceof Number) {
                        total += ((Number) value).intValue();
                    } else if (value.getClass().isArray()) {
                        total += Array.getLength(value);
                    } else if (value instanceof Collection) {
                        total += ((Collection<?>) value).size();
                    } else if (value instanceof Map) {
                        total += ((Map<?, ?>) value).size();
                    } else {
                        total += 1;
                    }
                }
            }
        }
        return total;
    }

    public static List<String> sortSimpleName(List<String> list) {
        if (list != null && list.size() > 0) {
            Collections.sort(list, SIMPLE_NAME_COMPARATOR);
        }
        return list;
    }

    public static String getSimpleName(String name) {
        if (name != null && name.length() > 0) {
            final int ip = name.indexOf('/');
            String v = ip != -1 ? name.substring(0, ip + 1) : "";

            int i = name.lastIndexOf(':');
            int j = (i >= 0 ? name.lastIndexOf('.', i) : name.lastIndexOf('.'));
            if (j >= 0) {
                name = name.substring(j + 1);
            }
            name = v + name;
        }
        return name;
    }

}
