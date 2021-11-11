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
package org.apache.dubbo.rpc.filter.tps;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StatItemTest {

    private StatItem statItem;

    @AfterEach
    public void tearDown() throws Exception {
        statItem = null;
    }

    @Test
    public void testIsAllowable() throws Exception {
        statItem = new StatItem("test", 5, 1000L);
        long lastResetTime = statItem.getLastResetTime();
        assertTrue(statItem.isAllowable());
        Thread.sleep(1100L);
        assertTrue(statItem.isAllowable());
        assertTrue(lastResetTime != statItem.getLastResetTime());
        assertEquals(4, statItem.getToken());
    }

	@Test
	public void testAccuracy() throws Exception {
		final int EXPECTED_RATE = 5;
		statItem = new StatItem("test", EXPECTED_RATE, 60_000L);
		for (int i = 1; i <= EXPECTED_RATE; i++) {
			assertEquals(true, statItem.isAllowable());
		}

		// Must block the 6th item
		assertEquals(false, statItem.isAllowable());
	}
}
