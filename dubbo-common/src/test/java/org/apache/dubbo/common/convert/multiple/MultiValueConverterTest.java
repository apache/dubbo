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

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TransferQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link MultiValueConverter} Test
 *
 * @since 2.7.8
 */
public class MultiValueConverterTest {

    @Test
    public void testFind() {
        MultiValueConverter converter = MultiValueConverter.find(String.class, String[].class);
        assertEquals(StringToArrayConverter.class, converter.getClass());

        converter = MultiValueConverter.find(String.class, BlockingDeque.class);
        assertEquals(StringToBlockingDequeConverter.class, converter.getClass());

        converter = MultiValueConverter.find(String.class, BlockingQueue.class);
        assertEquals(StringToBlockingQueueConverter.class, converter.getClass());

        converter = MultiValueConverter.find(String.class, Collection.class);
        assertEquals(StringToCollectionConverter.class, converter.getClass());

        converter = MultiValueConverter.find(String.class, Deque.class);
        assertEquals(StringToDequeConverter.class, converter.getClass());

        converter = MultiValueConverter.find(String.class, List.class);
        assertEquals(StringToListConverter.class, converter.getClass());

        converter = MultiValueConverter.find(String.class, NavigableSet.class);
        assertEquals(StringToNavigableSetConverter.class, converter.getClass());

        converter = MultiValueConverter.find(String.class, Queue.class);
        assertEquals(StringToQueueConverter.class, converter.getClass());

        converter = MultiValueConverter.find(String.class, Set.class);
        assertEquals(StringToSetConverter.class, converter.getClass());

        converter = MultiValueConverter.find(String.class, TransferQueue.class);
        assertEquals(StringToTransferQueueConverter.class, converter.getClass());
    }
}
