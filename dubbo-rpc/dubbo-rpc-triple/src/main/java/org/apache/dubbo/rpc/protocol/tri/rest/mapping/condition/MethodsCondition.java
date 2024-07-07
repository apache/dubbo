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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.apache.dubbo.remoting.http12.HttpMethods.GET;
import static org.apache.dubbo.remoting.http12.HttpMethods.HEAD;
import static org.apache.dubbo.remoting.http12.HttpMethods.OPTIONS;

public final class MethodsCondition implements Condition<MethodsCondition, HttpRequest> {

    private final Set<String> methods;

    public MethodsCondition(String... methods) {
        this.methods = new HashSet<>(Arrays.asList(methods));
    }

    private MethodsCondition(Set<String> methods) {
        this.methods = methods;
    }

    public Set<String> getMethods() {
        return methods;
    }

    @Override
    public MethodsCondition combine(MethodsCondition other) {
        Set<String> set = new HashSet<>(methods);
        set.addAll(other.methods);
        return new MethodsCondition(set);
    }

    @Override
    public MethodsCondition match(HttpRequest request) {
        String method = request.method();
        if (methods.contains(method)) {
            return new MethodsCondition(method);
        }
        if (HEAD.name().equals(method) && methods.contains(GET.name())) {
            return new MethodsCondition(GET.name());
        }
        if (OPTIONS.name().equals(method)
                && request.hasHeader("origin")
                && request.hasHeader("access-control-request-method")) {
            return new MethodsCondition(OPTIONS.name());
        }

        return null;
    }

    @Override
    public int compareTo(MethodsCondition other, HttpRequest request) {
        if (other.methods.size() != methods.size()) {
            return other.methods.size() - methods.size();
        }
        if (methods.size() == 1) {
            if (methods.contains(HEAD.name()) && other.methods.contains(GET.name())) {
                return -1;
            }
            if (methods.contains(GET.name()) && other.methods.contains(HEAD.name())) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return methods.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != MethodsCondition.class) {
            return false;
        }
        return methods.equals(((MethodsCondition) obj).methods);
    }

    @Override
    public String toString() {
        return "MethodsCondition{methods=" + methods + '}';
    }
}
