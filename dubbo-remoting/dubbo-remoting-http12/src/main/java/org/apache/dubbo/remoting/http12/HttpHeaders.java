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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.message.DefaultHttpHeaders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

public interface HttpHeaders extends Iterable<Entry<CharSequence, String>> {

    int size();

    boolean isEmpty();

    boolean containsKey(CharSequence key);

    String getFirst(CharSequence name);

    List<String> get(CharSequence key);

    HttpHeaders add(CharSequence name, String value);

    HttpHeaders add(CharSequence name, Iterable<String> value);

    HttpHeaders add(CharSequence name, String... value);

    HttpHeaders add(Map<? extends CharSequence, ? extends Iterable<String>> map);

    HttpHeaders add(HttpHeaders headers);

    HttpHeaders set(CharSequence name, String value);

    HttpHeaders set(CharSequence name, Iterable<String> value);

    HttpHeaders set(CharSequence name, String... value);

    HttpHeaders set(Map<? extends CharSequence, ? extends Iterable<String>> map);

    HttpHeaders set(HttpHeaders headers);

    List<String> remove(CharSequence key);

    void clear();

    Set<String> names();

    Set<CharSequence> nameSet();

    Map<String, List<String>> asMap();

    @Override
    Iterator<Entry<CharSequence, String>> iterator();

    default Map<String, List<String>> toMap() {
        Map<String, List<String>> map = CollectionUtils.newLinkedHashMap(size());
        for (Entry<CharSequence, String> entry : this) {
            map.computeIfAbsent(entry.getKey().toString(), k -> new ArrayList<>(1))
                    .add(entry.getValue());
        }
        return map;
    }

    default void forEach(BiConsumer<String, String> action) {
        for (Entry<CharSequence, String> entry : this) {
            action.accept(entry.getKey().toString(), entry.getValue());
        }
    }

    static HttpHeaders create() {
        return new DefaultHttpHeaders();
    }
}
