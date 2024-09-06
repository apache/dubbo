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
package org.apache.dubbo.remoting.http12.netty4;

import java.util.Iterator;
import java.util.Map.Entry;

public final class StringValueIterator implements Iterator<Entry<CharSequence, String>> {

    private final Iterator<Entry<CharSequence, CharSequence>> iterator;

    public StringValueIterator(Iterator<Entry<CharSequence, CharSequence>> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Entry<CharSequence, String> next() {
        return new ValueEntry(iterator.next());
    }

    @Override
    public void remove() {
        iterator.remove();
    }

    private static final class ValueEntry implements Entry<CharSequence, String> {

        private final Entry<CharSequence, CharSequence> entry;
        private String value;

        ValueEntry(Entry<CharSequence, CharSequence> entry) {
            this.entry = entry;
        }

        @Override
        public CharSequence getKey() {
            return entry.getKey();
        }

        @Override
        public String getValue() {
            if (value == null) {
                CharSequence cs = entry.getValue();
                if (cs != null) {
                    value = cs.toString();
                }
            }
            return value;
        }

        @Override
        public String setValue(String value) {
            String old = getValue();
            entry.setValue(value);
            return old;
        }

        @Override
        public String toString() {
            return entry.toString();
        }
    }
}
