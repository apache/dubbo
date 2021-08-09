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

package org.apache.dubbo.rpc.cluster.router.state;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.router.tag.TagStaticStateRouter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;

public class TagStaticStateRouterTest {

    @Test
    public void testRoute() {
        URL url = Mockito.mock(URL.class);
        RouterChain routerChain = Mockito.mock(RouterChain.class);
        TagStaticStateRouter router = new TagStaticStateRouter(url, routerChain);

        Invoker<TagStaticStateRouterTest> invoker1 = Mockito.mock(Invoker.class);
        Invoker<TagStaticStateRouterTest> invoker2 = Mockito.mock(Invoker.class);
        Invoker<TagStaticStateRouterTest> invoker3 = Mockito.mock(Invoker.class);
        List<Invoker<TagStaticStateRouterTest>> invokerList = Arrays.asList(invoker1, invoker2, invoker3);

        BitList<Invoker<TagStaticStateRouterTest>> bitList = new BitList<>(invokerList, false);

        RouterCache<TagStaticStateRouterTest> cache = new RouterCache<>();

        Invocation invocation = new RpcInvocation();

        BitList<Invoker<TagStaticStateRouterTest>> invokerBitList = router.route(bitList, cache, url, invocation);
        Assertions.assertEquals(invokerBitList.getUnmodifiableList(), invokerList);

        ConcurrentHashMap<String, BitList<Invoker<TagStaticStateRouterTest>>> map = new ConcurrentHashMap<>();
        BitList<Invoker<TagStaticStateRouterTest>> bitList1 = new BitList<>(invokerBitList, true);
        BitList<Invoker<TagStaticStateRouterTest>> bitList2 = new BitList<>(invokerBitList, true);
        BitList<Invoker<TagStaticStateRouterTest>> bitList3 = new BitList<>(invokerBitList, true);
        bitList1.addIndex(0);
        bitList2.addIndex(1);
        bitList3.addIndex(2);
        map.put("tag1", bitList1);
        map.put("tag2", bitList2);
        map.put("tag3", bitList3);
        cache.setAddrPool(map);

        invocation.setAttachment(TAG_KEY, "tag1");
        invokerBitList = router.route(bitList, cache, url, invocation);
        Assertions.assertTrue(invokerBitList.contains(invoker1));

        invocation.setAttachment(TAG_KEY, "tag2");
        Assertions.assertTrue(invokerBitList.contains(invoker2));

        invocation.setAttachment(TAG_KEY, "tag3");
        Assertions.assertTrue(invokerBitList.contains(invoker3));
    }

    @Test
    public void testPool() {
        URL url = Mockito.mock(URL.class);
        RouterChain routerChain = Mockito.mock(RouterChain.class);
        TagStaticStateRouter router = new TagStaticStateRouter(url, routerChain);

        Invoker<TagStaticStateRouterTest> invoker1 = Mockito.mock(Invoker.class);
        Invoker<TagStaticStateRouterTest> invoker2 = Mockito.mock(Invoker.class);
        Invoker<TagStaticStateRouterTest> invoker3 = Mockito.mock(Invoker.class);
        List<Invoker<TagStaticStateRouterTest>> invokerList = Arrays.asList(invoker1, invoker2, invoker3);

        Mockito.when(invoker1.getUrl()).thenReturn(URL.valueOf("test://127.0.0.1/test?dubbo.tag=tag1"));
        Mockito.when(invoker2.getUrl()).thenReturn(URL.valueOf("test://127.0.0.1/test?dubbo.tag=tag2"));
        Mockito.when(invoker3.getUrl()).thenReturn(URL.valueOf("test://127.0.0.1/test"));

        RouterCache<TagStaticStateRouterTest> cache = router.pool(invokerList);
        ConcurrentMap<String, BitList<Invoker<TagStaticStateRouterTest>>> addrPool = cache.getAddrPool();
        Assertions.assertTrue(addrPool.get("tag1").contains(invoker1));
        Assertions.assertTrue(addrPool.get("tag2").contains(invoker2));
        Assertions.assertTrue(addrPool.get("noTag").contains(invoker3));
    }
}

