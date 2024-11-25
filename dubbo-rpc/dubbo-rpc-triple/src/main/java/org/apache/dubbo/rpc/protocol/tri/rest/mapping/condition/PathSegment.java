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

import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.PathParserException;
import org.apache.dubbo.rpc.protocol.tri.rest.util.KeyString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PathSegment implements Comparable<PathSegment> {

    private Type type;
    private String value;
    private List<String> variables;
    private Pattern pattern;

    private KeyString keyValue;

    public PathSegment(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public PathSegment(Type type, String value, String variable) {
        this.type = type;
        this.value = value;
        addVariable(variable);
    }

    public static PathSegment literal(String value) {
        return new PathSegment(Type.LITERAL, value);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }

    public String getVariable() {
        return variables.get(0);
    }

    public void addVariable(String variable) {
        if (variables == null) {
            variables = new ArrayList<>();
        } else if (variables.contains(variable)) {
            throw new PathParserException(Messages.DUPLICATE_CAPTURE_VARIABLE, variable);
        }
        variables.add(variable);
    }

    public Pattern getPattern() {
        if (pattern == null) {
            initPattern();
        }
        return pattern;
    }

    public void initPattern() {
        if (isPattern()) {
            pattern = Pattern.compile(value);
        }
    }

    public boolean isPattern() {
        return type == Type.PATTERN || type == Type.PATTERN_MULTI;
    }

    public boolean isTailMatching() {
        return type == Type.WILDCARD_TAIL || type == Type.PATTERN_MULTI;
    }

    public boolean match(KeyString path, int start, int end, Map<String, String> variableMap) {
        switch (type) {
            case SLASH:
            case LITERAL:
                if (keyValue == null) {
                    keyValue = new KeyString(value);
                }
                return path.regionMatches(start, keyValue);
            case WILDCARD_TAIL:
                if (variables != null) {
                    variableMap.put(getVariable(), path.toString(start));
                }
                return true;
            case VARIABLE:
                if (variables != null) {
                    variableMap.put(getVariable(), path.toString(start, end));
                }
                return true;
            case PATTERN:
                return matchPattern(path.toString(start, end), variableMap);
            case PATTERN_MULTI:
                return matchPattern(path.toString(start), variableMap);
            default:
                return false;
        }
    }

    public boolean match(String path, int start, int end, Map<String, String> variableMap) {
        return match(new KeyString(path), start, end, variableMap);
    }

    private boolean matchPattern(String path, Map<String, String> variableMap) {
        Matcher matcher = getPattern().matcher(path);
        if (matcher.matches()) {
            if (variables == null) {
                return true;
            }
            for (int i = 0, size = variables.size(); i < size; i++) {
                String variable = variables.get(i);
                variableMap.put(variable, matcher.group(variable));
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.ordinal() | (value == null ? 0 : value.hashCode() << 3);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != PathSegment.class) {
            return false;
        }
        PathSegment that = (PathSegment) obj;
        return type == that.type && value.equals(that.value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{type=");
        sb.append(type);
        if (value != null) {
            sb.append(", value=").append(value);
        }
        if (variables != null) {
            sb.append(", variables=").append(variables);
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(PathSegment other) {
        if (type != other.type) {
            int comparison = type.score() - other.type.score;
            if (comparison != 0) {
                return comparison;
            }
        }
        int size = variables == null ? 99 : variables.size();
        int otherSize = other.variables == null ? 99 : other.variables.size();
        return size - otherSize;
    }

    public enum Type {
        /**
         * A slash segment, transient type used for parsing.
         * will not be present in the PathExpression
         * E.g.: '/'
         */
        SLASH,
        /**
         * A literal segment.
         * E.g.: 'foo'
         */
        LITERAL(1),
        /**
         * A wildcard segment.
         * E.g.: 't?st*uv'
         */
        WILDCARD,
        /**
         * A wildcard matching suffix.
         * Transient type used for parsing, will not be present in the PathExpression
         * E.g.: '/foo/**' or '/**' or '/{*bar}'
         */
        WILDCARD_TAIL,
        /**
         * A template variable segment.
         * E.g.: '{foo}' or '/foo/&ast;/bar'
         */
        VARIABLE(10),
        /**
         * A regex variable matching single segment.
         * E.g.: '{foo:\d+}'
         */
        PATTERN(100),
        /**
         * A regex variable matching multiple segments.
         * E.g.: '{foo:.*}'
         */
        PATTERN_MULTI(200);

        private final int score;

        Type(int score) {
            this.score = score;
        }

        Type() {
            score = 10000;
        }

        public int score() {
            return score;
        }
    }
}
