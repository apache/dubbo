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
package org.apache.dubbo.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.utils.StringUtils.EMPTY_STRING;
import static org.apache.dubbo.common.utils.StringUtils.decodeHexByte;
import static org.apache.dubbo.common.utils.Utf8Utils.decodeUtf8;

public final class URLStrParser {

    private static final char SPACE = 0x20;

    private static final ThreadLocal<TempBuf> DECODE_TEMP_BUF = ThreadLocal.withInitial(() -> new TempBuf(1024));

    private URLStrParser() {
        //empty
    }

    /**
     * @param decodedURLStr : after {@link URL#decode} string
     *                      decodedURLStr format: protocol://username:password@host:port/path?k1=v1&k2=v2
     *                      [protocol://][username:password@][host:port]/[path][?k1=v1&k2=v2]
     */
    public static URL parseDecodedStr(String decodedURLStr) {
        Map<String, String> parameters = null;
        int pathEndIdx = decodedURLStr.indexOf('?');
        if (pathEndIdx >= 0) {
            parameters = parseDecodedParams(decodedURLStr, pathEndIdx + 1);
        } else {
            pathEndIdx = decodedURLStr.length();
        }

        String decodedBody = decodedURLStr.substring(0, pathEndIdx);
        return parseURLBody(decodedURLStr, decodedBody, parameters);
    }

    private static Map<String, String> parseDecodedParams(String str, int from) {
        int len = str.length();
        if (from >= len) {
            return Collections.emptyMap();
        }

        TempBuf tempBuf = DECODE_TEMP_BUF.get();
        Map<String, String> params = new HashMap<>();
        int nameStart = from;
        int valueStart = -1;
        int i;
        for (i = from; i < len; i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case '=':
                    if (nameStart == i) {
                        nameStart = i + 1;
                    } else if (valueStart < nameStart) {
                        valueStart = i + 1;
                    }
                    break;
                case ';':
                case '&':
                    addParam(str, false, nameStart, valueStart, i, params, tempBuf);
                    nameStart = i + 1;
                    break;
                default:
                    // continue
            }
        }
        addParam(str, false, nameStart, valueStart, i, params, tempBuf);
        return params;
    }

    /**
     * @param fullURLStr  : fullURLString
     * @param decodedBody : format: [protocol://][username:password@][host:port]/[path]
     * @param parameters  :
     * @return URL
     */
    private static URL parseURLBody(String fullURLStr, String decodedBody, Map<String, String> parameters) {
        int starIdx = 0, endIdx = decodedBody.length();
        String protocol = null;
        int protoEndIdx = decodedBody.indexOf("://");
        if (protoEndIdx >= 0) {
            if (protoEndIdx == 0) {
                throw new IllegalStateException("url missing protocol: \"" + fullURLStr + "\"");
            }
            protocol = decodedBody.substring(0, protoEndIdx);
            starIdx = protoEndIdx + 3;
        } else {
            // case: file:/path/to/file.txt
            protoEndIdx = decodedBody.indexOf(":/");
            if (protoEndIdx >= 0) {
                if (protoEndIdx == 0) {
                    throw new IllegalStateException("url missing protocol: \"" + fullURLStr + "\"");
                }
                protocol = decodedBody.substring(0, protoEndIdx);
                starIdx = protoEndIdx + 1;
            }
        }

        String path = null;
        int pathStartIdx = indexOf(decodedBody, '/', starIdx, endIdx);
        if (pathStartIdx >= 0) {
            path = decodedBody.substring(pathStartIdx + 1);
            endIdx = pathStartIdx;
        }

        String username = null;
        String password = null;
        int pwdEndIdx = lastIndexOf(decodedBody, '@', starIdx, endIdx);
        if (pwdEndIdx > 0) {
            int userNameEndIdx = indexOf(decodedBody, ':', starIdx, pwdEndIdx);
            username = decodedBody.substring(starIdx, userNameEndIdx);
            password = decodedBody.substring(userNameEndIdx + 1, pwdEndIdx);
            starIdx = pwdEndIdx + 1;
        }

        String host = null;
        int port = 0;
        int hostEndIdx = lastIndexOf(decodedBody, ':', starIdx, endIdx);
        if (hostEndIdx > 0 && hostEndIdx < decodedBody.length() - 1) {
            if (lastIndexOf(decodedBody, '%', starIdx, endIdx) > hostEndIdx) {
                // ipv6 address with scope id
                // e.g. fe80:0:0:0:894:aeec:f37d:23e1%en0
                // see https://howdoesinternetwork.com/2013/ipv6-zone-id
                // ignore
            } else {
                port = Integer.parseInt(decodedBody.substring(hostEndIdx + 1, endIdx));
                endIdx = hostEndIdx;
            }
        }

        if (endIdx > starIdx) {
            host = decodedBody.substring(starIdx, endIdx);
        }
        return new URL(protocol, username, password, host, port, path, parameters);
    }

    /**
     * @param encodedURLStr : after {@link URL#encode(String)} string
     *                      encodedURLStr after decode format: protocol://username:password@host:port/path?k1=v1&k2=v2
     *                      [protocol://][username:password@][host:port]/[path][?k1=v1&k2=v2]
     */
    public static URL parseEncodedStr(String encodedURLStr) {
        Map<String, String> parameters = null;
        int pathEndIdx = encodedURLStr.indexOf("%3F");// '?'
        if (pathEndIdx >= 0) {
            parameters = parseEncodedParams(encodedURLStr, pathEndIdx + 3);
        } else {
            pathEndIdx = encodedURLStr.length();
        }

        //decodedBody format: [protocol://][username:password@][host:port]/[path]
        String decodedBody = decodeComponent(encodedURLStr, 0, pathEndIdx, false, DECODE_TEMP_BUF.get());
        return parseURLBody(encodedURLStr, decodedBody, parameters);
    }

    private static Map<String, String> parseEncodedParams(String str, int from) {
        int len = str.length();
        if (from >= len) {
            return Collections.emptyMap();
        }

        TempBuf tempBuf = DECODE_TEMP_BUF.get();
        Map<String, String> params = new HashMap<>();
        int nameStart = from;
        int valueStart = -1;
        int i;
        for (i = from; i < len; i++) {
            char ch = str.charAt(i);
            if (ch == '%') {
                if (i + 3 > len) {
                    throw new IllegalArgumentException("unterminated escape sequence at index " + i + " of: " + str);
                }
                ch = (char) decodeHexByte(str, i + 1);
                i += 2;
            }

            switch (ch) {
                case '=':
                    if (nameStart == i) {
                        nameStart = i + 1;
                    } else if (valueStart < nameStart) {
                        valueStart = i + 1;
                    }
                    break;
                case ';':
                case '&':
                    addParam(str, true, nameStart, valueStart, i - 2, params, tempBuf);
                    nameStart = i + 1;
                    break;
                default:
                    // continue
            }
        }
        addParam(str, true, nameStart, valueStart, i, params, tempBuf);
        return params;
    }

    private static boolean addParam(String str, boolean isEncoded, int nameStart, int valueStart, int valueEnd, Map<String, String> params,
                                    TempBuf tempBuf) {
        if (nameStart >= valueEnd) {
            return false;
        }

        if (valueStart <= nameStart) {
            valueStart = valueEnd + 1;
        }

        if (isEncoded) {
            String name = decodeComponent(str, nameStart, valueStart - 3, false, tempBuf);
            String value = decodeComponent(str, valueStart, valueEnd, false, tempBuf);
            params.put(name, value);
        } else {
            String name = str.substring(nameStart, valueStart -1);
            String value = str.substring(valueStart, valueEnd);
            params.put(name, value);
        }
        return true;
    }

    private static String decodeComponent(String s, int from, int toExcluded, boolean isPath, TempBuf tempBuf) {
        int len = toExcluded - from;
        if (len <= 0) {
            return EMPTY_STRING;
        }

        int firstEscaped = -1;
        for (int i = from; i < toExcluded; i++) {
            char c = s.charAt(i);
            if (c == '%' || c == '+' && !isPath) {
                firstEscaped = i;
                break;
            }
        }
        if (firstEscaped == -1) {
            return s.substring(from, toExcluded);
        }

        // Each encoded byte takes 3 characters (e.g. "%20")
        int decodedCapacity = (toExcluded - firstEscaped) / 3;
        byte[] buf = tempBuf.byteBuf(decodedCapacity);
        char[] charBuf = tempBuf.charBuf(len);
        s.getChars(from, firstEscaped, charBuf, 0);

        int charBufIdx = firstEscaped - from;
        return decodeUtf8Component(s, firstEscaped, toExcluded, isPath, buf, charBuf, charBufIdx);
    }

    private static String decodeUtf8Component(String str, int firstEscaped, int toExcluded, boolean isPath, byte[] buf,
                                              char[] charBuf, int charBufIdx) {
        int bufIdx;
        for (int i = firstEscaped; i < toExcluded; i++) {
            char c = str.charAt(i);
            if (c != '%') {
                charBuf[charBufIdx++] = c != '+' || isPath ? c : SPACE;
                continue;
            }

            bufIdx = 0;
            do {
                if (i + 3 > toExcluded) {
                    throw new IllegalArgumentException("unterminated escape sequence at index " + i + " of: " + str);
                }
                buf[bufIdx++] = decodeHexByte(str, i + 1);
                i += 3;
            } while (i < toExcluded && str.charAt(i) == '%');
            i--;

            charBufIdx += decodeUtf8(buf, 0, bufIdx, charBuf, charBufIdx);
        }
        return new String(charBuf, 0, charBufIdx);
    }

    private static int indexOf(String str, char ch, int from, int toExclude) {
        from = Math.max(from, 0);
        toExclude = Math.min(toExclude, str.length());
        if (from > toExclude) {
            return -1;
        }

        for (int i = from; i < toExclude; i++) {
            if (str.charAt(i) == ch) {
                return i;
            }
        }
        return -1;
    }

    private static int lastIndexOf(String str, char ch, int from, int toExclude) {
        from = Math.max(from, 0);
        toExclude = Math.min(toExclude, str.length() - 1);
        if (from > toExclude) {
            return -1;
        }

        for (int i = toExclude; i >= from; i--) {
            if (str.charAt(i) == ch) {
                return i;
            }
        }
        return -1;
    }

    private static final class TempBuf {

        private final char[] chars;

        private final byte[] bytes;

        TempBuf(int bufSize) {
            this.chars = new char[bufSize];
            this.bytes = new byte[bufSize];
        }

        public char[] charBuf(int size) {
            char[] chars = this.chars;
            if (size <= chars.length) {
                return chars;
            }
            return new char[size];
        }

        public byte[] byteBuf(int size) {
            byte[] bytes = this.bytes;
            if (size <= bytes.length) {
                return bytes;
            }
            return new byte[size];
        }
    }
}
