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

import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathSegment.Type;
import org.apache.dubbo.rpc.protocol.tri.rest.util.KeyString;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PathExpression implements Comparable<PathExpression> {

    private final String path;
    private final PathSegment[] segments;

    private PathExpression(String path, PathSegment[] segments) {
        this.path = path;
        this.segments = segments;
    }

    public static PathExpression parse(@Nonnull String path) {
        return new PathExpression(path, PathParser.parse(path));
    }

    public static boolean match(@Nonnull String path, String value) {
        return value != null && parse(path).match(value) != null;
    }

    public String getPath() {
        return path;
    }

    public PathSegment[] getSegments() {
        return segments;
    }

    public boolean isDirect() {
        return segments.length == 1 && segments[0].getType() == Type.LITERAL;
    }

    public Map<String, String> match(@Nonnull String path) {
        if (isDirect()) {
            return this.path.equals(path) ? Collections.emptyMap() : null;
        }
        Map<String, String> variableMap = new LinkedHashMap<>();
        int start, end = 0;
        for (int i = 0, len = segments.length; i < len; i++) {
            PathSegment segment = segments[i];
            if (end != -1) {
                start = end + 1;
                end = path.indexOf('/', start);
                if (segment.match(new KeyString(path), start, end, variableMap)) {
                    if (i == len - 1 && segment.isTailMatching()) {
                        return variableMap;
                    }
                    continue;
                }
            }
            return null;
        }
        return end == -1 ? variableMap : null;
    }

    public int compareTo(PathExpression other, String lookupPath) {
        boolean equalsPath = path.equals(lookupPath);
        boolean otherEqualsPath = other.path.equals(lookupPath);
        if (equalsPath) {
            return otherEqualsPath ? 0 : -1;
        }
        if (otherEqualsPath) {
            return 1;
        }
        return compareTo(other);
    }

    @Override
    public int compareTo(PathExpression other) {
        int size = segments.length;
        int otherSize = other.segments.length;
        if (isDirect() && other.isDirect()) {
            return other.path.length() - path.length();
        }
        for (int i = 0; i < size && i < otherSize; i++) {
            int result = segments[i].compareTo(other.segments[i]);
            if (result != 0) {
                return result;
            }
        }
        return size - otherSize;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != PathExpression.class) {
            return false;
        }
        return path.equals(((PathExpression) obj).path);
    }

    @Override
    public String toString() {
        if (isDirect()) {
            return path;
        }
        StringBuilder sb = new StringBuilder(path.length());
        int varIndex = 1;
        for (PathSegment segment : segments) {
            sb.append('/');
            switch (segment.getType()) {
                case LITERAL:
                    sb.append(segment.getValue());
                    break;
                case WILDCARD_TAIL:
                    List<String> variables = segment.getVariables();
                    if (variables == null) {
                        sb.append("{path}");
                    } else {
                        sb.append('{').append(variables.get(0)).append('}');
                    }
                    break;
                case VARIABLE:
                    sb.append('{');
                    String value = segment.getValue();
                    if (value.isEmpty()) {
                        sb.append("var").append(varIndex++);
                    } else {
                        sb.append(value);
                    }
                    sb.append('}');
                    break;
                case PATTERN:
                case PATTERN_MULTI:
                    sb.append('{').append("var").append(varIndex++).append('}');
                    break;
                default:
                    break;
            }
        }
        return sb.toString();
    }
}
