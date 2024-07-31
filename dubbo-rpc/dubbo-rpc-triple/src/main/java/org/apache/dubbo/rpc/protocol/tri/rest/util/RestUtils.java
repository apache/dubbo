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
package org.apache.dubbo.rpc.protocol.tri.rest.util;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestExtension;

public final class RestUtils {

    private RestUtils() {}

    public static boolean hasPlaceholder(String text) {
        if (text == null) {
            return false;
        }
        int len = text.length();
        if (len < 4) {
            return false;
        }
        int state = 0;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '$') {
                state = 1;
            } else if (c == '{') {
                if (state == 1) {
                    if (text.charAt(i + 1) != '$') {
                        return false;
                    }
                    state = 2;
                }
            } else if (c == '}' && state == 2) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMaybeJSONObject(String str) {
        if (str == null) {
            return false;
        }
        int i = 0, n = str.length();
        if (n < 3) {
            return false;
        }
        char expected = 0;
        for (; i < n; i++) {
            char c = str.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c == '{') {
                expected = '}';
                break;
            }
            return false;
        }
        for (int j = n - 1; j > i; j--) {
            char c = str.charAt(j);
            if (Character.isWhitespace(c)) {
                continue;
            }
            return c == expected;
        }
        return false;
    }

    public static int getPriority(Object obj) {
        if (obj instanceof Prioritized) {
            int priority = ((Prioritized) obj).getPriority();
            if (priority != 0) {
                return priority;
            }
        }
        Activate activate = obj.getClass().getAnnotation(Activate.class);
        return activate == null ? 0 : activate.order();
    }

    public static String[] getPattens(Object obj) {
        return obj instanceof RestExtension ? ((RestExtension) obj).getPatterns() : null;
    }
}
