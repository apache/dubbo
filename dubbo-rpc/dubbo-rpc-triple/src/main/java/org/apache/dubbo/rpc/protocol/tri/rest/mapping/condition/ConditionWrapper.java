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

import org.apache.dubbo.remoting.http12.HttpRequest;

@SuppressWarnings("unchecked")
public final class ConditionWrapper implements Condition<ConditionWrapper, HttpRequest> {

    private final Condition<Object, HttpRequest> condition;

    public ConditionWrapper(Condition<?, HttpRequest> condition) {
        this.condition = (Condition<Object, HttpRequest>) condition;
    }

    @Override
    public ConditionWrapper combine(ConditionWrapper other) {
        return new ConditionWrapper((Condition<?, HttpRequest>) condition.combine(other.condition));
    }

    @Override
    public ConditionWrapper match(HttpRequest request) {
        Condition<?, HttpRequest> match = (Condition<?, HttpRequest>) condition.match(request);
        return match == null ? null : new ConditionWrapper(match);
    }

    @Override
    public int compareTo(ConditionWrapper other, HttpRequest request) {
        return condition.compareTo(other.condition, request);
    }

    public Condition<Object, HttpRequest> getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return condition.toString();
    }
}
