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

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.LinkedHashMap;
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

    public String getPath() {
        return path;
    }

    public PathSegment[] getSegments() {
        return segments;
    }

    public boolean isDirect() {
        return segments.length == 1 && segments[0].getType() == PathSegment.Type.LITERAL;
    }

    public Map<String, String> match(@Nonnull String path) {
        if (isDirect()) {
            return this.path.equals(path) ? Collections.emptyMap() : null;
        }
        Map<String, String> variableMap = new LinkedHashMap<>();
        int start, end = 0;
        for (PathSegment segment : segments) {
            if (end != -1) {
                start = end + 1;
                end = path.indexOf('/', start);
                if (segment.match(path, start, end, variableMap)) {
                    continue;
                }
            }
            return null;
        }
        return variableMap;
    }

    @Override
    public int compareTo(PathExpression other) {
        return 0;
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
        if (obj == null || PathExpression.class != obj.getClass()) {
            return false;
        }
        PathExpression that = (PathExpression) obj;
        return path.equals(that.path);
    }

    @Override
    public String toString() {
        return path;
    }
}
