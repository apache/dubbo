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
package org.apache.dubbo.common.threadpool.manager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Ring<T> {

    AtomicInteger count = new AtomicInteger();

    private List<T> itemList = new CopyOnWriteArrayList<T>();

    public void addItem(T t) {
        if (t != null) {
            itemList.add(t);
        }
    }

    public T pollItem() {
        if (itemList.isEmpty()) {
            return null;
        }
        if (itemList.size() == 1) {
            return itemList.get(0);
        }

        if (count.intValue() > Integer.MAX_VALUE - 10000) {
            count.set(count.get() % itemList.size());
        }

        int index = Math.abs(count.getAndIncrement()) % itemList.size();
        return itemList.get(index);
    }

    public T peekItem() {
        if (itemList.isEmpty()) {
            return null;
        }
        if (itemList.size() == 1) {
            return itemList.get(0);
        }
        int index = Math.abs(count.get()) % itemList.size();
        return itemList.get(index);
    }

    public List<T> listItems() {
        return Collections.unmodifiableList(itemList);
    }
}
