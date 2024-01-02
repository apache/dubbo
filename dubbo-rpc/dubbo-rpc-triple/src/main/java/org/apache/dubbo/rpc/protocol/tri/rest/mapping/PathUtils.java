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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathParserException;

import javax.annotation.Nonnull;

public final class PathUtils {
    private PathUtils() {}

    public static boolean isDirectPath(@Nonnull String path) {
        boolean braceStart = false;
        for (int i = 0, len = path.length(); i < len; i++) {
            switch (path.charAt(i)) {
                case '*':
                case '?':
                    return false;
                case '{':
                    braceStart = true;
                    continue;
                case '}':
                    if (braceStart) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    /**
     * See
     * <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/util/AntPathMatcher.html#combine(java.lang.String,java.lang.String)">AntPathMatcher#combine</a>
     */
    public static String combine(@Nonnull String path1, @Nonnull String path2) {
        if (path1.isEmpty()) {
            return path2.isEmpty() ? StringUtils.EMPTY_STRING : path2;
        }
        if (path2.isEmpty()) {
            return path1;
        }

        int len1 = path1.length();
        char last1 = path1.charAt(len1 - 1);
        if (len1 == 1) {
            if (last1 == '/' || last1 == '*') {
                return path2;
            }
        } else if (path1.indexOf('{') == -1) {
            int starDotPos1 = path1.lastIndexOf("*.");
            if (starDotPos1 > -1) {
                String ext1 = path1.substring(starDotPos1 + 1);
                int dotPos2 = path2.lastIndexOf('.');
                String file2, ext2;
                if (dotPos2 == -1) {
                    file2 = path2;
                    ext2 = StringUtils.EMPTY_STRING;
                } else {
                    file2 = path2.substring(0, dotPos2);
                    ext2 = path2.substring(dotPos2);
                }
                boolean ext1All = ext1.equals(".*");
                boolean ext2All = ext2.equals(".*") || ext2.isEmpty();
                if (!ext1All && !ext2All) {
                    throw new PathParserException(Messages.CANNOT_COMBINE_PATHS, path1, path2);
                }
                return file2 + (ext1All ? ext2 : ext1);
            }
        }
        if (last1 == '*') {
            if (path1.charAt(len1 - 2) == '/') {
                path1 = path1.substring(0, len1 - 2);
            }
        }

        boolean slash1 = last1 == '/';
        boolean slash2 = path2.charAt(0) == '/';
        if (slash1) {
            return slash2 ? path1 + path2.substring(1) : path1 + path2;
        }
        if (slash2) {
            return path1 + path2;
        }
        return path1 + '/' + path2;
    }

    public static String normalize(@Nonnull String path) {
        int state = State.INITIAL;
        int start = -1, end = 0;
        int len = path.length();
        StringBuilder buf = null;
        out:
        for (int i = 0; i < len; i++) {
            char c = path.charAt(i);
            switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                    continue;
                case '/':
                    switch (state) {
                        case State.SLASH:
                            if (start != -1) {
                                if (buf == null) {
                                    buf = new StringBuilder(len);
                                }
                                buf.append(path, start, i);
                            }
                            start = -1;
                            continue;
                        case State.DOT:
                            state = State.SLASH;
                            if (buf == null) {
                                buf = new StringBuilder(len);
                            }
                            buf.append(path, start, i - 1);
                            start = -1;
                            continue;
                        case State.DOT_DOT:
                            state = State.SLASH;
                            if (buf == null) {
                                buf = new StringBuilder(len);
                            }
                            if (end > 2) {
                                buf.append(path, start, i - 2);
                                int bLen = buf.length();
                                if (bLen > 1) {
                                    int pos = buf.lastIndexOf(RestConstants.SLASH, bLen - 2);
                                    if (!"/../".equals(buf.substring(pos))) {
                                        buf.setLength(pos + 1);
                                        start = -1;
                                        continue;
                                    }
                                }
                            }
                            buf.append(path, start, i + 1);
                            start = -1;
                            continue;
                        default:
                            state = State.SLASH;
                            break;
                    }
                    break;
                case '.':
                    switch (state) {
                        case State.INITIAL:
                            if (buf == null) {
                                buf = new StringBuilder(len);
                            }
                            buf.append('/');
                        case State.SLASH:
                            state = State.DOT;
                            break;
                        case State.DOT:
                            state = State.DOT_DOT;
                            break;
                        case State.DOT_DOT:
                            state = State.NORMAL;
                            break;
                    }
                    break;
                case '?':
                case '#':
                    break out;
                default:
                    switch (state) {
                        case State.INITIAL:
                            if (buf == null) {
                                buf = new StringBuilder(len);
                            }
                            buf.append('/');
                        case State.SLASH:
                        case State.DOT:
                        case State.DOT_DOT:
                            state = State.NORMAL;
                            break;
                    }
                    break;
            }
            if (start == -1) {
                start = i;
            }
            end = i;
        }
        if (buf == null) {
            return path;
        }
        if (start != -1) {
            buf.append(path, start, end + 1);
        }
        return buf.toString();
    }

    private interface State {

        int INITIAL = 0;
        int NORMAL = 1;
        int SLASH = 2;
        int DOT = 3;
        int DOT_DOT = 4;
    }
}
