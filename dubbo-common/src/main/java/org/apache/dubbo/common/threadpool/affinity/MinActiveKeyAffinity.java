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
package org.apache.dubbo.common.threadpool.affinity;

import java.util.Comparator;
import java.util.function.Supplier;

public class MinActiveKeyAffinity<K, V> extends AbstractKeyAffinity<K, V> {

    public MinActiveKeyAffinity(Supplier<V> v, int count) {
        super(v, count);
    }

    @Override
    AbstractKeyAffinity<K, V>.KeyRef selectKeyIfNotExists(K key) {
        if (getAll().isEmpty()) {
            KeyRef keyRef = genRandomKeyRef();
            putVal(key, keyRef);
            return keyRef;
        }
        return getAll()
                .stream()
                .min(Comparator.comparingInt(ValueRef::concurrency))
                .map(KeyRef::new)
                .orElseThrow(IllegalStateException::new);
    }
}
