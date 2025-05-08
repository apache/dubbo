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

import java.util.function.Function;

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
                    if (text.charAt(i - 1) != '$') {
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

    public static String replacePlaceholder(String text, Function<String, String> resolver) {
        if (text == null) {
            return null;
        }
        int len = text.length();
        if (len < 2) {
            return text;
        }

        int p = 0, nameStart = 0, nameEnd = 0, valueStart = 0, valueEnd = 0;
        String value;
        StringBuilder buf = null;
        int state = State.START;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '$':
                    if (state == State.START) {
                        if (buf == null) {
                            buf = new StringBuilder(len);
                        }
                        buf.append(text, p, i);
                        p = i;
                        state = State.DOLLAR;
                    } else if (state == State.DOLLAR) {
                        if (buf == null) {
                            buf = new StringBuilder(len);
                        }
                        buf.append(text, p, i - 1);
                        p = i;
                        state = State.START;
                    }
                    break;
                case '{':
                    state = state == State.DOLLAR ? State.BRACE_OPEN : State.START;
                    break;
                case ':':
                    state = state == State.NAME_START ? State.COLON : State.START;
                    break;
                case '}':
                    switch (state) {
                        case State.DOLLAR:
                        case State.BRACE_OPEN:
                            state = State.START;
                            break;
                        case State.COLON:
                            state = State.START;
                            valueStart = i;
                            break;
                        case State.DOLLAR_NAME_START:
                        case State.NAME_START:
                        case State.VALUE_START:
                            value = resolver.apply(text.substring(nameStart, nameEnd));
                            if (buf == null) {
                                buf = new StringBuilder(len);
                            }
                            if (value == null) {
                                if (state == State.VALUE_START) {
                                    buf.append(text, valueStart, valueEnd);
                                } else {
                                    buf.append(text, p, i + 1);
                                }
                            } else {
                                buf.append(value);
                            }
                            p = i + 1;
                            state = State.START;
                            break;
                        default:
                    }
                    break;
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                    if (state == State.DOLLAR_NAME_START) {
                        state = State.START;
                        value = resolver.apply(text.substring(nameStart, nameEnd));
                        if (buf == null) {
                            buf = new StringBuilder(len);
                        }
                        if (value == null) {
                            buf.append(text, p, i);
                        } else {
                            buf.append(value);
                        }
                        p = i;
                    }
                    break;
                default:
                    switch (state) {
                        case State.DOLLAR:
                            state = State.DOLLAR_NAME_START;
                            nameStart = i;
                            break;
                        case State.BRACE_OPEN:
                            state = State.NAME_START;
                            nameStart = i;
                            break;
                        case State.COLON:
                            state = State.VALUE_START;
                            valueStart = i;
                            break;
                        case State.DOLLAR_NAME_START:
                        case State.NAME_START:
                            nameEnd = i + 1;
                            break;
                        case State.VALUE_START:
                            valueEnd = i + 1;
                            break;
                        default:
                    }
            }
        }
        if (state == State.DOLLAR_NAME_START) {
            value = resolver.apply(text.substring(nameStart, nameEnd));
            if (buf == null) {
                buf = new StringBuilder(len);
            }
            if (value == null) {
                buf.append(text, p, len);
            } else {
                buf.append(value);
            }
        } else {
            if (buf == null) {
                return text;
            }
            buf.append(text, p, len);
        }
        return buf.toString();
    }

    private interface State {

        int START = 0;
        int DOLLAR = 1;
        int BRACE_OPEN = 2;
        int COLON = 3;
        int DOLLAR_NAME_START = 4;
        int NAME_START = 5;
        int VALUE_START = 6;
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
