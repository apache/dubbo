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

import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ConsumesCondition implements Condition<ConsumesCondition, HttpRequest> {

    public static final MediaTypeExpression DEFAULT = MediaTypeExpression.parse("application/octet-stream");

    private final List<MediaTypeExpression> expressions;

    public ConsumesCondition(String... consumes) {
        this(consumes, null);
    }

    public ConsumesCondition(String[] consumes, String[] headers) {
        Set<MediaTypeExpression> expressions = null;
        if (headers != null) {
            for (String header : headers) {
                NameValueExpression expr = NameValueExpression.parse(header);
                if (HttpHeaderNames.CONTENT_TYPE.getName().equalsIgnoreCase(expr.getName())) {
                    MediaTypeExpression expression = MediaTypeExpression.parse(expr.getValue());
                    if (expression == null) {
                        continue;
                    }
                    if (expressions == null) {
                        expressions = new LinkedHashSet<>();
                    }
                    expressions.add(expression);
                }
            }
        }
        if (consumes != null) {
            for (String consume : consumes) {
                MediaTypeExpression expression = MediaTypeExpression.parse(consume);
                if (expression == null) {
                    continue;
                }
                if (expressions == null) {
                    expressions = new LinkedHashSet<>();
                }
                expressions.add(expression);
            }
        }
        if (expressions == null) {
            this.expressions = Collections.emptyList();
        } else {
            this.expressions = new ArrayList<>(expressions);
            Collections.sort(this.expressions);
        }
    }

    private ConsumesCondition(List<MediaTypeExpression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public ConsumesCondition combine(ConsumesCondition other) {
        return other.expressions.isEmpty() ? this : other;
    }

    @Override
    public ConsumesCondition match(HttpRequest request) {
        if (expressions.isEmpty()) {
            return null;
        }

        String contentType = request.contentType();
        MediaTypeExpression mediaType = contentType == null ? DEFAULT : MediaTypeExpression.parse(contentType);
        List<MediaTypeExpression> result = null;
        for (int i = 0, size = expressions.size(); i < size; i++) {
            MediaTypeExpression expression = expressions.get(i);
            if (expression.match(mediaType)) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(expression);
            }
        }
        return result == null ? null : new ConsumesCondition(result);
    }

    @Override
    public int compareTo(ConsumesCondition other, HttpRequest request) {
        if (expressions.isEmpty()) {
            return other.expressions.isEmpty() ? 0 : 1;
        }
        if (other.expressions.isEmpty()) {
            return -1;
        }
        return expressions.get(0).compareTo(other.expressions.get(0));
    }

    public List<MediaType> getMediaTypes() {
        return MediaTypeExpression.toMediaTypes(expressions);
    }

    @Override
    public int hashCode() {
        return expressions.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != ConsumesCondition.class) {
            return false;
        }
        return expressions.equals(((ConsumesCondition) obj).expressions);
    }

    @Override
    public String toString() {
        return "ConsumesCondition{mediaTypes=" + expressions + '}';
    }
}
