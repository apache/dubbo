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
package org.apache.dubbo.config.integration.multiple;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This abstraction class to implement the basic methods for {@link Storage}.
 *
 * @param <T> The type to store
 */
public abstract class AbstractStorage<T> implements Storage<T> {

    private Map<String, T> storage = new ConcurrentHashMap<>();


    /**
     * Generate the key for storage
     *
     * @param host the host in the register center.
     * @param port the port in the register center.
     * @return the generated key with the given host and port.
     */
    private String generateKey(String host, int port) {
        return String.format("%s:%d", host, port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get(String host, int port) {
        return storage.get(generateKey(host, port));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String host, int port, T value) {
        storage.put(generateKey(host, port), value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(String host, int port) {
        return storage.containsKey(generateKey(host, port));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return storage.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        storage.clear();
    }
}
