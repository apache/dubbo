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
package org.apache.dubbo.remoting.exchange.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * {@link MultiMessage}
 */
class MultiMessageTest {

    @Test
    void test() {
        MultiMessage multiMessage = MultiMessage.create();
        Assertions.assertTrue(multiMessage instanceof Iterable);

        multiMessage.addMessage("test1");
        multiMessage.addMessages(Arrays.asList("test2", "test3"));
        Assertions.assertEquals(multiMessage.size(), 3);
        Assertions.assertFalse(multiMessage.isEmpty());
        Assertions.assertEquals(multiMessage.get(0), "test1");
        Assertions.assertEquals(multiMessage.get(1), "test2");
        Assertions.assertEquals(multiMessage.get(2), "test3");

        Collection messages = multiMessage.getMessages();
        Assertions.assertTrue(messages.contains("test1"));
        Assertions.assertTrue(messages.contains("test2"));
        Assertions.assertTrue(messages.contains("test3"));

        Iterator iterator = messages.iterator();
        Assertions.assertTrue(iterator.hasNext());
        Assertions.assertEquals(iterator.next(), "test1");
        Assertions.assertEquals(iterator.next(), "test2");
        Assertions.assertEquals(iterator.next(), "test3");

        Collection removedCollection = multiMessage.removeMessages();
        Assertions.assertArrayEquals(removedCollection.toArray(), messages.toArray());
        messages = multiMessage.getMessages();
        Assertions.assertTrue(messages.isEmpty());

        MultiMessage multiMessage1 = MultiMessage.createFromCollection(Arrays.asList("test1", "test2"));
        MultiMessage multiMessage2 = MultiMessage.createFromArray("test1", "test2");
        Assertions.assertArrayEquals(multiMessage1.getMessages().toArray(), multiMessage2.getMessages().toArray());

    }

}
