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

package org.apache.dubbo.rpc.protocol.tri;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

public class DefaultMetadata implements Metadata {
    private final Map<CharSequence, CharSequence> innerMap = new HashMap<>();

    @Override
    public CharSequence get(CharSequence key) {
        return innerMap.get(key);
    }

    @Override
    public Metadata put(CharSequence key, CharSequence value) {
        innerMap.put(key, value);
        return this;
    }

    @Override
    public Iterator<Map.Entry<CharSequence, CharSequence>> iterator() {
        return innerMap.entrySet().iterator();
    }

    @Override
    public void forEach(Consumer<? super Map.Entry<CharSequence, CharSequence>> action) {
        innerMap.entrySet().forEach(action);
    }

    @Override
    public Spliterator<Map.Entry<CharSequence, CharSequence>> spliterator() {
        return innerMap.entrySet().spliterator();
    }

    @Override
    public boolean contains(CharSequence key) {
        return innerMap.containsKey(key);
    }

    @Override
    public boolean remove(CharSequence key) {
        innerMap.remove(key);
        return true;
    }

}
