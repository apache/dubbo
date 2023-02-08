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
package org.apache.dubbo.common.convert.multiple;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TransferQueue;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StringToQueueConverter} Test
 *
 * @since 2.7.6
 */
class StringToQueueConverterTest {

    private StringToQueueConverter converter;

    @BeforeEach
    public void init() {
        converter = new StringToQueueConverter(FrameworkModel.defaultModel());
    }

    @Test
    void testAccept() {

        assertFalse(converter.accept(String.class, Collection.class));

        assertFalse(converter.accept(String.class, List.class));
        assertFalse(converter.accept(String.class, AbstractList.class));
        assertTrue(converter.accept(String.class, LinkedList.class));
        assertFalse(converter.accept(String.class, ArrayList.class));

        assertTrue(converter.accept(String.class, Queue.class));
        assertTrue(converter.accept(String.class, BlockingQueue.class));
        assertTrue(converter.accept(String.class, TransferQueue.class));
        assertTrue(converter.accept(String.class, Deque.class));
        assertTrue(converter.accept(String.class, BlockingDeque.class));

        assertFalse(converter.accept(String.class, Set.class));
        assertFalse(converter.accept(String.class, SortedSet.class));
        assertFalse(converter.accept(String.class, NavigableSet.class));
        assertFalse(converter.accept(String.class, TreeSet.class));
        assertFalse(converter.accept(String.class, ConcurrentSkipListSet.class));

        assertFalse(converter.accept(null, char[].class));
        assertFalse(converter.accept(null, String.class));
        assertFalse(converter.accept(null, String.class));
        assertFalse(converter.accept(null, null));
    }

    @Test
    void testConvert() {

        Queue values = new ArrayDeque(asList(1.0, 2.0, 3.0));

        Queue result = (Queue<Double>) converter.convert("1.0,2.0,3.0", Queue.class, Double.class);

        assertTrue(CollectionUtils.equals(values, result));

        values.clear();
        values.add(123);

        result = (Queue) converter.convert("123", Queue.class, Integer.class);

        assertTrue(CollectionUtils.equals(values, result));

        assertNull(converter.convert(null, Collection.class, Integer.class));
        assertNull(converter.convert("", Collection.class, null));
    }

    @Test
    void testGetSourceType() {
        assertEquals(String.class, converter.getSourceType());
    }

    @Test
    void testGetPriority() {
        assertEquals(Integer.MAX_VALUE - 2, converter.getPriority());
    }
}