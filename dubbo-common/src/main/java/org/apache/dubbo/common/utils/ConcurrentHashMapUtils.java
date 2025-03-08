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
package org.apache.dubbo.common.utils;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * ConcurrentHashMap util
 */
public class ConcurrentHashMapUtils {

    /**
     * A temporary workaround for Java 8 ConcurrentHashMap#computeIfAbsent specific performance issue: JDK-8161372.</br>
     *
     * @see <a href="https://bugs.openjdk.java.net/browse/JDK-8161372">https://bugs.openjdk.java.net/browse/JDK-8161372</a>
     */
    public static <K, V> V computeIfAbsent(ConcurrentMap<K, V> map, K key, Function<? super K, ? extends V> func) {
        return computeIfAbsent(map, key, func, null);
    }

    public static <K, V> V computeIfAbsent(
            ConcurrentMap<K, V> map, K key, Function<? super K, ? extends V> func, Consumer<V> threadSafeOperation) {
        Objects.requireNonNull(func);
        V value;
        if (JRE.JAVA_8.isCurrentVersion()) {
            V v = map.get(key);
            if (null == v) {
                // issue#11986 lock bug
                // v = map.computeIfAbsent(key, func);

                // this bug fix methods maybe cause `func.apply` multiple calls.
                v = func.apply(key);
                if (null == v) {
                    return null;
                }
                final V res = map.putIfAbsent(key, v);
                if (null != res) {
                    // if pre value present, means other thread put value already, and putIfAbsent not effect
                    // return exist value
                    value = res;
                } else {
                    value = v;
                }
                // if pre value is null, means putIfAbsent effected, return current value
            } else {
                value = v;
            }
        } else {
            value = map.computeIfAbsent(key, func);
        }
        if (value != null && threadSafeOperation != null) {
            // make sure value operations are thread - safe.
            synchronized (value) {
                threadSafeOperation.accept(value);
            }
        }
        return value;
    }
}
