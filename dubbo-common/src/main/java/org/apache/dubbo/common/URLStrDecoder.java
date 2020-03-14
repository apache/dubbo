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

import java.util.*;

import static org.apache.dubbo.common.utils.StringUtils.EMPTY_STRING;
import static org.apache.dubbo.common.utils.StringUtils.decodeHexByte;
import static org.apache.dubbo.common.utils.Utf8Utils.decodeUtf8;

public class URLStrDecoder {

    private static final char SPACE = 0x20;

    private static final ThreadLocal<TempBuf> DECODE_TEMP_BUF = ThreadLocal.withInitial(() -> new TempBuf(1024));

    private final String encodedURLStr;

    public URLStrDecoder(String encodedURLStr) {
        this.encodedURLStr = encodedURLStr;
    }

    //url decode 后的格式: protocol://username:password@host:port/path?k1=v1&k2=v2
    // [protocol://][username:password@][host:port]/[path][?k1=v1&k2=v2]
    public URL decode() {
        Map<String, String> parameters = null;
        int pathEndIdx = encodedURLStr.indexOf("%3F");// '?'
        if (pathEndIdx >= 0) {
            parameters = decodeParams(encodedURLStr, pathEndIdx + 3);
        } else {
            pathEndIdx = encodedURLStr.length();
        }

        TempBuf tempBuf = DECODE_TEMP_BUF.get();

        //decodedURLBody format: [protocol://][username:password@][host:port]/[path]
        String decodedURLBody = decodeComponent(encodedURLStr, 0, pathEndIdx, false, tempBuf);

        int searchStartIdx = 0;
        String protocol = null;
        int protocolEndIdx = decodedURLBody.indexOf("://");
        if (protocolEndIdx >= 0) {
            if (protocolEndIdx == 0) {
                throw new IllegalStateException("url missing protocol: \"" + encodedURLStr + "\"");
            }
            protocol = decodedURLBody.substring(0, protocolEndIdx);
            searchStartIdx = protocolEndIdx + 3;
        } else {
            // case: file:/path/to/file.txt
            protocolEndIdx = decodedURLBody.indexOf(":/");
            if (protocolEndIdx >= 0) {
                if (protocolEndIdx == 0) {
                    throw new IllegalStateException("url missing protocol: \"" + encodedURLStr + "\"");
                }
                protocol = decodedURLBody.substring(0, protocolEndIdx);
                searchStartIdx = protocolEndIdx + 1;
            }
        }

        String path = null;
        int pathStartIdx = decodedURLBody.indexOf('/', searchStartIdx);
        if (pathStartIdx >= 0) {
            path = decodedURLBody.substring(pathStartIdx + 1);
        } else {
            pathStartIdx = decodedURLBody.length();//no path
        }

        String username = null;
        String password = null;
        int pwdEndIdx = decodedURLBody.lastIndexOf('@', pathStartIdx);
        if (pwdEndIdx > 0) {
            int userNameEndIdx = decodedURLBody.indexOf(':', searchStartIdx);
            username = decodedURLBody.substring(searchStartIdx, userNameEndIdx);
            password = decodedURLBody.substring(userNameEndIdx + 1, pwdEndIdx);
            searchStartIdx = pwdEndIdx + 1;
        }

        String host = null;
        int port = 0;
        int searchEndIdx = pathStartIdx;
        int hostEndIdx = decodedURLBody.lastIndexOf(':');
        if (hostEndIdx > 0 && hostEndIdx < decodedURLBody.length() - 1) {
            if (decodedURLBody.lastIndexOf('%') > hostEndIdx) {
                // ipv6 address with scope id
                // e.g. fe80:0:0:0:894:aeec:f37d:23e1%en0
                // see https://howdoesinternetwork.com/2013/ipv6-zone-id
                // ignore
            } else {
                port = Integer.parseInt(decodedURLBody.substring(hostEndIdx + 1, searchEndIdx));
                searchEndIdx = hostEndIdx;
            }
        }

        if (searchEndIdx > searchStartIdx) {
            host = decodedURLBody.substring(searchStartIdx, searchEndIdx);
        }
        return new URL(protocol, username, password, host, port, path, parameters);
    }

    private static Map<String, String> decodeParams(String str, int from) {
        int len = str.length();
        if (from >= len) {
            return Collections.emptyMap();
        }

//        if (str.charAt(from) == '?') {
//            from++;
//        }

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
                    addParam(str, nameStart, valueStart, i - 2, params, tempBuf);
                    nameStart = i + 1;
                    break;
                default:
                    // continue
            }
        }
        addParam(str, nameStart, valueStart, i, params, tempBuf);
        return params;
    }

    private static boolean addParam(String str, int nameStart, int valueStart, int valueEnd,
                                    Map<String, String> params, TempBuf tempBuf) {
        if (nameStart >= valueEnd) {
            return false;
        }

        if (valueStart <= nameStart) {
            valueStart = valueEnd + 1;
        }

        String name = decodeComponent(str, nameStart, valueStart - 3, false, tempBuf);
        String value = decodeComponent(str, valueStart, valueEnd, false, tempBuf);
        params.put(name, value);
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

    private static String decodeUtf8Component(String str, int firstEscaped, int toExcluded, boolean isPath,
                                              byte[] buf, char[] charBuf, int charBufIdx) {
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
