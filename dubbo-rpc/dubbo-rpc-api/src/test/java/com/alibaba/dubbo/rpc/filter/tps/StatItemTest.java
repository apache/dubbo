/*
 * Copyright 1999-2012 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.dubbo.rpc.filter.tps;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.RpcInvocation;

import org.junit.After;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class StatItemTest {

    private StatItem statItem;

    private URL url = URL.valueOf("test://localhost");

    private Invocation invocation = new RpcInvocation();

    @After
    public void tearDown() throws Exception {
        statItem = null;
    }

    @Test
    public void testIsAllowable() throws Exception {
        statItem = new StatItem("test", 5, 1000L);
        long lastResetTime = statItem.getLastResetTime();
        assertEquals(true, statItem.isAllowable(url, invocation));
        Thread.sleep(1100L);
        assertEquals(true, statItem.isAllowable(url, invocation));
        assertTrue(lastResetTime != statItem.getLastResetTime());
        assertEquals(4, statItem.getToken());
    }

}
