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
package org.apache.dubbo.remoting.http12.command;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the write queue's batch callback and command ordering.
 *
 * @date 2026-11-10
 */
class HttpWriteQueueTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 129, 130, 259})
    void flushesEachChunkAfterRunningCommandsInOrder(int count) {
        List<Runnable> tasks = new ArrayList<>();
        List<Integer> writes = new ArrayList<>();
        List<Integer> flushes = new ArrayList<>();
        List<HttpChannelQueueCommand> commands = new ArrayList<>();
        HttpWriteQueue queue = new HttpWriteQueue(tasks::add, () -> flushes.add(writes.size()));
        for (int i = 0; i < count; i++) {
            int index = i;
            HttpChannelQueueCommand command = new HttpChannelQueueCommand() {
                @Override
                public void run() {
                    writes.add(index);
                    complete(null);
                }
            };
            commands.add(command);
            assertEquals(command, queue.enqueue(command));
            assertFalse(command.isDone());
        }
        assertEquals(1, tasks.size(), "all queued commands share one scheduled drain");
        tasks.get(0).run();
        assertEquals(count, writes.size());
        for (int i = 0; i < count; i++) {
            assertEquals(i, writes.get(i));
            assertTrue(commands.get(i).isDone());
            assertFalse(commands.get(i).isCompletedExceptionally());
        }
        List<Integer> expectedFlushes = new ArrayList<>();
        for (int boundary = 129; boundary < count; boundary += 129) {
            expectedFlushes.add(boundary);
        }
        expectedFlushes.add(count);
        assertEquals(expectedFlushes, flushes);
    }
}
