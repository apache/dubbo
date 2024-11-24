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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.PathParserException;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathSegment.Type;
import org.apache.dubbo.rpc.protocol.tri.rest.util.PathUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * See
 * <p>
 * <a href="https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html#mvc-ann-requestmapping-uri-templates">Spring uri templates</a>
 * <br/>
 * <a href="https://docs.jboss.org/resteasy/docs/6.2.7.Final/userguide/html/ch04.html">Path and regular expression mappings</a>
 * </p>
 */
final class PathParser {

    private static final PathSegment SLASH = new PathSegment(Type.SLASH, RestConstants.SLASH);

    private final List<PathSegment> segments = new LinkedList<>();
    private final StringBuilder buf = new StringBuilder();

    /**
     * Ensure that the path is normalized using {@link PathUtils#normalize(String)} before parsing.
     */
    static PathSegment[] parse(String path) {
        if (path == null || path.isEmpty() || RestConstants.SLASH.equals(path)) {
            return new PathSegment[] {PathSegment.literal(RestConstants.SLASH)};
        }
        if (PathUtils.isDirectPath(path)) {
            return new PathSegment[] {PathSegment.literal(path)};
        }
        List<PathSegment> segments = new PathParser().doParse(path);
        return segments.toArray(new PathSegment[0]);
    }

    private List<PathSegment> doParse(String path) {
        parseSegments(path);
        transformSegments(segments, path);
        for (PathSegment segment : segments) {
            try {
                segment.initPattern();
            } catch (Exception e) {
                throw new PathParserException(Messages.REGEX_PATTERN_INVALID, segment.getValue(), path, e);
            }
        }
        return segments;
    }

    private void parseSegments(String path) {
        int state = State.INITIAL;
        boolean regexBraceStart = false;
        boolean regexMulti = false;
        String variableName = null;
        int len = path.length();
        for (int i = 0; i < len; i++) {
            char c = path.charAt(i);
            switch (c) {
                case '/':
                    switch (state) {
                        case State.INITIAL:
                        case State.SEGMENT_END:
                            continue;
                        case State.LITERAL_START:
                            if (buf.length() > 0) {
                                appendSegment(Type.LITERAL);
                            }
                            break;
                        case State.WILDCARD_START:
                            appendSegment(Type.WILDCARD);
                            break;
                        case State.REGEX_VARIABLE_START:
                            if (path.charAt(i - 1) != '^' || path.charAt(i - 2) != '[') {
                                regexMulti = true;
                            }
                            buf.append(c);
                            continue;
                        case State.VARIABLE_START:
                        case State.WILDCARD_VARIABLE_START:
                            throw new PathParserException(Messages.MISSING_CLOSE_CAPTURE, path, i);
                        default:
                    }
                    segments.add(SLASH);
                    state = State.SEGMENT_END;
                    continue;
                case '?':
                    switch (state) {
                        case State.INITIAL:
                        case State.LITERAL_START:
                        case State.SEGMENT_END:
                            state = State.WILDCARD_START;
                            break;
                        default:
                    }
                    break;
                case '*':
                    switch (state) {
                        case State.INITIAL:
                        case State.LITERAL_START:
                        case State.SEGMENT_END:
                            state = State.WILDCARD_START;
                            break;
                        case State.VARIABLE_START:
                            if (path.charAt(i - 1) == '{') {
                                state = State.WILDCARD_VARIABLE_START;
                                continue;
                            }
                            break;
                        default:
                    }
                    break;
                case '.':
                    if (state == State.REGEX_VARIABLE_START) {
                        if (path.charAt(i - 1) != '\\') {
                            regexMulti = true;
                        }
                    }
                    break;
                case 'S':
                case 'W':
                    if (state == State.REGEX_VARIABLE_START) {
                        if (path.charAt(i - 1) == '\\') {
                            regexMulti = true;
                        }
                    }
                    break;
                case ':':
                    if (state == State.VARIABLE_START) {
                        state = State.REGEX_VARIABLE_START;
                        variableName = buf.toString();
                        buf.setLength(0);
                        continue;
                    }
                    break;
                case '{':
                    switch (state) {
                        case State.INITIAL:
                        case State.SEGMENT_END:
                            state = State.VARIABLE_START;
                            continue;
                        case State.LITERAL_START:
                            if (buf.length() > 0) {
                                appendSegment(Type.LITERAL);
                            }
                            state = State.VARIABLE_START;
                            continue;
                        case State.VARIABLE_START:
                        case State.WILDCARD_VARIABLE_START:
                            throw new PathParserException(Messages.ILLEGAL_NESTED_CAPTURE, path, i);
                        case State.REGEX_VARIABLE_START:
                            if (path.charAt(i - 1) != '\\') {
                                regexBraceStart = true;
                            }
                            break;
                        default:
                    }
                    break;
                case '}':
                    switch (state) {
                        case State.INITIAL:
                        case State.LITERAL_START:
                        case State.SEGMENT_END:
                            throw new PathParserException(Messages.MISSING_OPEN_CAPTURE, path);
                        case State.VARIABLE_START:
                            appendSegment(Type.VARIABLE, buf.toString());
                            state = State.LITERAL_START;
                            continue;
                        case State.REGEX_VARIABLE_START:
                            if (regexBraceStart) {
                                regexBraceStart = false;
                            } else {
                                if (buf.length() == 0) {
                                    throw new PathParserException(Messages.MISSING_REGEX_CONSTRAINT, path, i);
                                }
                                appendSegment(regexMulti ? Type.PATTERN_MULTI : Type.PATTERN, variableName);
                                regexMulti = false;
                                state = State.LITERAL_START;
                                continue;
                            }
                            break;
                        case State.WILDCARD_VARIABLE_START:
                            appendSegment(Type.WILDCARD_TAIL, buf.toString());
                            state = State.END;
                            continue;
                        default:
                    }
                    break;
                default:
                    if (state == State.INITIAL || state == State.SEGMENT_END) {
                        state = State.LITERAL_START;
                    }
                    break;
            }
            if (state == State.END) {
                throw new PathParserException(Messages.NO_MORE_DATA_ALLOWED, path, i);
            }
            buf.append(c);
        }

        if (buf.length() > 0) {
            switch (state) {
                case State.LITERAL_START:
                    appendSegment(Type.LITERAL);
                    break;
                case State.WILDCARD_START:
                    appendSegment(Type.WILDCARD);
                    break;
                case State.VARIABLE_START:
                case State.REGEX_VARIABLE_START:
                case State.WILDCARD_VARIABLE_START:
                    throw new PathParserException(Messages.MISSING_CLOSE_CAPTURE, path, len - 1);
                default:
            }
        }
    }

    private void appendSegment(Type type) {
        segments.add(new PathSegment(type, buf.toString()));
        buf.setLength(0);
    }

    private void appendSegment(Type type, String name) {
        segments.add(new PathSegment(type, buf.toString().trim(), name.trim()));
        buf.setLength(0);
    }

    private static void transformSegments(List<PathSegment> segments, String path) {
        ListIterator<PathSegment> iterator = segments.listIterator();
        PathSegment curr, prev = null;
        while (iterator.hasNext()) {
            curr = iterator.next();
            String value = curr.getValue();
            Type type = curr.getType();
            switch (type) {
                case SLASH:
                    if (prev != null) {
                        switch (prev.getType()) {
                            case LITERAL:
                            case VARIABLE:
                            case PATTERN:
                                prev = curr;
                                break;
                            case PATTERN_MULTI:
                                if (!".*".equals(prev.getValue())) {
                                    prev.setValue(prev.getValue() + '/');
                                }
                                break;
                            default:
                        }
                    }
                    iterator.remove();
                    continue;
                case WILDCARD:
                    if ("*".equals(value)) {
                        type = Type.VARIABLE;
                        value = StringUtils.EMPTY_STRING;
                    } else if ("**".equals(value)) {
                        if (!iterator.hasNext()) {
                            type = Type.WILDCARD_TAIL;
                            value = StringUtils.EMPTY_STRING;
                        } else {
                            type = Type.PATTERN_MULTI;
                            value = ".*";
                        }
                    } else {
                        type = Type.PATTERN;
                        value = toRegex(value);
                    }
                    curr.setType(type);
                    curr.setValue(value);
                    break;
                case WILDCARD_TAIL:
                    break;
                case PATTERN:
                case PATTERN_MULTI:
                    curr.setValue("(?<" + curr.getVariable() + '>' + value + ')');
                    break;
                default:
            }
            if (prev == null) {
                prev = curr;
                continue;
            }
            String pValue = prev.getValue();
            switch (prev.getType()) {
                case LITERAL:
                    switch (type) {
                        case VARIABLE:
                            prev.setType(Type.PATTERN);
                            prev.setValue(quoteRegex(pValue) + "(?<" + curr.getVariable() + ">[^/]+)");
                            prev.setVariables(curr.getVariables());
                            iterator.remove();
                            continue;
                        case PATTERN:
                        case PATTERN_MULTI:
                            prev.setType(type);
                            prev.setValue(quoteRegex(pValue) + "(?<" + curr.getVariable() + '>' + value + ')');
                            prev.setVariables(curr.getVariables());
                            iterator.remove();
                            continue;
                        default:
                    }
                    break;
                case VARIABLE:
                    switch (type) {
                        case LITERAL:
                            prev.setType(Type.PATTERN);
                            prev.setValue("(?<" + prev.getVariable() + ">[^/]+)" + quoteRegex(value));
                            iterator.remove();
                            continue;
                        case VARIABLE:
                            throw new PathParserException(Messages.ILLEGAL_DOUBLE_CAPTURE, path);
                        case PATTERN:
                        case PATTERN_MULTI:
                            String var = curr.getVariable();
                            prev.addVariable(var);
                            prev.setType(type);
                            prev.setValue("(?<" + prev.getVariable() + ">[^/]+)(?<" + var + '>' + value + ')');
                            iterator.remove();
                            continue;
                        default:
                    }
                    break;
                case PATTERN:
                case PATTERN_MULTI:
                    switch (type) {
                        case LITERAL:
                            prev.setValue(pValue + quoteRegex(value));
                            iterator.remove();
                            continue;
                        case WILDCARD_TAIL:
                            if (curr.getVariables() == null) {
                                prev.setValue(pValue + ".*");
                            } else {
                                prev.addVariable(curr.getVariable());
                                prev.setValue(pValue + "(?<" + curr.getVariable() + ">.*)");
                            }
                            prev.setType(Type.PATTERN_MULTI);
                            iterator.remove();
                            continue;
                        case VARIABLE:
                            if (value.isEmpty()) {
                                prev.setValue(pValue + "[^/]+");
                                iterator.remove();
                                continue;
                            }
                            prev.addVariable(curr.getVariable());
                            prev.setValue(pValue + "(?<" + curr.getVariable() + ">[^/]+)");
                            iterator.remove();
                            continue;
                        case PATTERN_MULTI:
                            prev.setType(Type.PATTERN_MULTI);
                        case PATTERN:
                            if (curr.getVariables() == null) {
                                prev.setValue(pValue + value);
                            } else {
                                prev.addVariable(curr.getVariable());
                                prev.setValue(pValue + "(?<" + curr.getVariable() + '>' + value + ')');
                            }
                            iterator.remove();
                            continue;
                        default:
                    }
                    break;
                default:
            }
            prev = curr;
        }
    }

    private static String quoteRegex(String regex) {
        for (int i = 0, len = regex.length(); i < len; i++) {
            switch (regex.charAt(i)) {
                case '(':
                case ')':
                case '[':
                case ']':
                case '$':
                case '^':
                case '.':
                case '{':
                case '}':
                case '|':
                case '\\':
                    return "\\Q" + regex + "\\E";
                default:
            }
        }
        return regex;
    }

    private static String toRegex(String wildcard) {
        int len = wildcard.length();
        StringBuilder sb = new StringBuilder(len + 8);
        for (int i = 0; i < len; i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    if (i > 0) {
                        char prev = wildcard.charAt(i - 1);
                        if (prev == '*') {
                            continue;
                        }
                        if (prev == '?') {
                            sb.append("*");
                            continue;
                        }
                    }
                    sb.append("[^/]*");
                    break;
                case '?':
                    if (i > 0 && wildcard.charAt(i - 1) == '*') {
                        continue;
                    }
                    sb.append("[^/]");
                    break;
                case '(':
                case ')':
                case '$':
                case '.':
                case '{':
                case '}':
                case '|':
                case '\\':
                    sb.append('\\');
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    private interface State {

        int INITIAL = 0;
        int LITERAL_START = 1;
        int WILDCARD_START = 2;
        int VARIABLE_START = 3;
        int REGEX_VARIABLE_START = 4;
        int WILDCARD_VARIABLE_START = 5;
        int SEGMENT_END = 6;
        int END = 7;
    }
}
