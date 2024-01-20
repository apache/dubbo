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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.protocol.tri.rest.util.PathUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PathCondition implements Condition<PathCondition, HttpRequest> {

    private final String contextPath;
    private final Set<String> paths;
    private List<PathExpression> expressions;

    public PathCondition(String contextPath, String... paths) {
        this.contextPath = contextPath;
        this.paths = new LinkedHashSet<>(Arrays.asList(paths));
    }

    private PathCondition(String contextPath, Set<String> paths) {
        this.contextPath = contextPath;
        this.paths = paths;
    }

    public List<PathExpression> getExpressions() {
        if (expressions == null) {
            expressions = new ArrayList<>();
            for (String path : paths) {
                expressions.add(PathExpression.parse(PathUtils.normalize(contextPath, path)));
            }
        }
        return expressions;
    }

    @Override
    public PathCondition combine(PathCondition other) {
        Set<String> result = new LinkedHashSet<>();
        if (paths.isEmpty()) {
            if (other.paths.isEmpty()) {
                result.add(StringUtils.EMPTY_STRING);
            } else {
                result.addAll(other.paths);
            }
        } else {
            if (other.paths.isEmpty()) {
                result.addAll(paths);
            } else {
                for (String left : paths) {
                    for (String right : other.paths) {
                        result.add(PathUtils.combine(left, right));
                    }
                }
            }
        }
        return new PathCondition(contextPath, result);
    }

    @Override
    public PathCondition match(HttpRequest request) {
        List<PathExpression> matches = null;
        String path = request.rawPath();
        List<PathExpression> expressions = getExpressions();
        for (int i = 0, size = expressions.size(); i < size; i++) {
            PathExpression expression = expressions.get(i);
            Map<String, String> variables = expression.match(path);
            if (variables != null) {
                if (matches == null) {
                    matches = new ArrayList<>();
                }
                matches.add(expression);
            }
        }
        if (matches != null) {
            Collections.sort(matches);
            Set<String> result = CollectionUtils.newLinkedHashSet(matches.size());
            for (int i = 0, size = matches.size(); i < size; i++) {
                result.add(matches.get(i).getPath());
            }
            return new PathCondition(contextPath, result);
        }
        return null;
    }

    @Override
    public int compareTo(PathCondition other, HttpRequest request) {
        return 0;
    }

    @Override
    public String toString() {
        return "PathCondition{paths=" + paths + '}';
    }
}
