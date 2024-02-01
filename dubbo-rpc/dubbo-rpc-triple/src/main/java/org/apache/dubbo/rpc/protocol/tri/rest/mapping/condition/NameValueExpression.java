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

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class NameValueExpression {

    private final String name;
    private final String value;
    private final boolean negated;

    private NameValueExpression(String name, String value, boolean negated) {
        this.name = name;
        this.value = value;
        this.negated = negated;
    }

    public NameValueExpression(String name, String value) {
        this.name = name;
        this.value = value;
        negated = false;
    }

    public static Set<NameValueExpression> parse(String... params) {
        if (ArrayUtils.isEmpty(params)) {
            return Collections.emptySet();
        }
        int len = params.length;
        Set<NameValueExpression> expressions = CollectionUtils.newHashSet(len);
        for (String param : params) {
            expressions.add(parse(param));
        }
        return expressions;
    }

    public static NameValueExpression parse(String expr) {
        int index = expr.indexOf('=');
        if (index == -1) {
            boolean negated = expr.indexOf('!') == 0;
            return new NameValueExpression(negated ? expr.substring(1) : expr, null, negated);
        } else {
            boolean negated = index > 0 && expr.charAt(index - 1) == '!';
            return new NameValueExpression(
                    negated ? expr.substring(0, index - 1) : expr.substring(0, index),
                    expr.substring(index + 1),
                    negated);
        }
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean match(Predicate<String> nameFn, Function<String, String> valueFn) {
        boolean matched;
        if (value == null) {
            matched = nameFn.test(name);
        } else {
            matched = Objects.equals(valueFn.apply(name), value);
        }
        return matched != negated;
    }

    public boolean match(Function<String, String> valueFn) {
        return match(n -> valueFn.apply(n) != null, valueFn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, negated);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NameValueExpression.class) {
            return false;
        }
        NameValueExpression other = (NameValueExpression) obj;
        return negated == other.negated && Objects.equals(name, other.name) && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        return name + (negated ? "!=" : "=") + value;
    }
}
