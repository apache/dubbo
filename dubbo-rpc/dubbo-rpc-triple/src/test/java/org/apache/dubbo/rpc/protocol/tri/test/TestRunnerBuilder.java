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
package org.apache.dubbo.rpc.protocol.tri.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class TestRunnerBuilder {

    private final List<TProvider<?>> providers = new ArrayList<>();

    public static TestRunnerBuilder builder() {
        return new TestRunnerBuilder();
    }

    public static TestRunnerBuilder of(Object service) {
        return builder().provider(service);
    }

    public static <T> TestRunnerBuilder of(Class<T> type, T service) {
        return builder().provider(type, service);
    }

    public <T> TestRunnerBuilder provider(Class<T> type, T service, Map<String, String> parameters) {
        providers.add(new TProvider<>(requireNonNull(type), requireNonNull(service), parameters));
        return this;
    }

    public <T> TestRunnerBuilder provider(Class<T> type, T service) {
        return provider(type, service, Collections.emptyMap());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public TestRunnerBuilder provider(Object service, Map<String, String> parameters) {
        requireNonNull(service);
        Class<?>[] interfaces = service.getClass().getInterfaces();
        providers.add(new TProvider(interfaces[0], service, parameters));
        return this;
    }

    public TestRunnerBuilder provider(Object service) {
        return provider(service, Collections.emptyMap());
    }

    public TestRunner build() {
        return new TestRunnerImpl(providers);
    }

    static final class TProvider<T> {
        final Class<T> type;
        final T service;
        final Map<String, String> parameters;

        TProvider(Class<T> type, T service, Map<String, String> parameters) {
            this.type = type;
            this.service = service;
            this.parameters = parameters;
        }
    }
}
