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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class HeadersCondition implements Condition<HeadersCondition, HttpRequest> {

    private final Set<NameValueExpression> expressions;

    public HeadersCondition(String... headers) {
        Set<NameValueExpression> expressions = null;
        if (headers != null) {
            for (String header : headers) {
                NameValueExpression expr = NameValueExpression.parse(header);
                String name = expr.getName();
                if (HttpHeaderNames.ACCEPT.getName().equalsIgnoreCase(name)
                        || HttpHeaderNames.CONTENT_TYPE.getName().equalsIgnoreCase(name)) {
                    continue;
                }
                if (expressions == null) {
                    expressions = new LinkedHashSet<>();
                }
                expressions.add(expr);
            }
        }
        this.expressions = expressions == null ? Collections.emptySet() : expressions;
    }

    private HeadersCondition(Set<NameValueExpression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public HeadersCondition combine(HeadersCondition other) {
        Set<NameValueExpression> set = new LinkedHashSet<>(expressions);
        set.addAll(other.expressions);
        return new HeadersCondition(set);
    }

    @Override
    public HeadersCondition match(HttpRequest request) {
        for (NameValueExpression expression : expressions) {
            if (!expression.match(request::hasHeader, request::header)) {
                return null;
            }
        }
        return this;
    }

    @Override
    public int compareTo(HeadersCondition other, HttpRequest request) {
        return other.expressions.size() - expressions.size();
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
        if (obj == null || obj.getClass() != HeadersCondition.class) {
            return false;
        }
        return expressions.equals(((HeadersCondition) obj).expressions);
    }

    @Override
    public String toString() {
        return "HeadersCondition{headers=" + expressions + '}';
    }
}
