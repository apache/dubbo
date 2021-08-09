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
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.router.tag.TagDynamicStateRouter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;

public class TagDynamicStateRouterTest {

    Invoker<TagDynamicStateRouterTest> invoker1 = Mockito.mock(Invoker.class);
    Invoker<TagDynamicStateRouterTest> invoker2 = Mockito.mock(Invoker.class);
    Invoker<TagDynamicStateRouterTest> invoker3 = Mockito.mock(Invoker.class);
    List<Invoker<TagDynamicStateRouterTest>> invokerList = Arrays.asList(invoker1, invoker2, invoker3);

    @Test
    public void testPool() {
        URL url = Mockito.mock(URL.class);
        RouterChain routerChain = Mockito.mock(RouterChain.class);
        TagDynamicStateRouter router = new TagDynamicStateRouter(url, routerChain);

        RouterCache<TagDynamicStateRouterTest> cache = router.pool(invokerList);
        Assertions.assertEquals(cache.getAddrPool().get("noTag").getUnmodifiableList(), invokerList);

        cache = buildParsedRuleRouterCache(router);

        Assertions.assertTrue(cache.getAddrPool().get("tag1").size() == 1);
        Assertions.assertTrue(cache.getAddrPool().get("tag2").size() == 1);
        Assertions.assertTrue(cache.getAddrPool().get("noTag").size() == 1);

        Assertions.assertTrue(cache.getAddrPool().get("tag1").contains(invoker1));
        Assertions.assertTrue(cache.getAddrPool().get("tag2").contains(invoker2));
        Assertions.assertTrue(cache.getAddrPool().get("noTag").contains(invoker3));
    }

    @Test
    public void testRoute() {
        URL url = Mockito.mock(URL.class);
        RouterChain routerChain = Mockito.mock(RouterChain.class);
        TagDynamicStateRouter router = new TagDynamicStateRouter(url, routerChain);

        RouterCache<TagDynamicStateRouterTest> cache = buildParsedRuleRouterCache(router);

        BitList<Invoker<TagDynamicStateRouterTest>> invokerBitList = new BitList<>(invokerList, false);
        RpcInvocation invocation = new RpcInvocation();

        BitList<Invoker<TagDynamicStateRouterTest>> bitList = router.route(invokerBitList, cache, url, invocation);
        Assertions.assertTrue(bitList.contains(invoker3));

        invocation.setAttachment(TAG_KEY,"tag1");
        bitList = router.route(invokerBitList, cache, url, invocation);
        Assertions.assertTrue(bitList.contains(invoker1));


        invocation.setAttachment(TAG_KEY,"tag2");
        bitList = router.route(invokerBitList, cache, url, invocation);
        Assertions.assertTrue(bitList.contains(invoker2));
    }

    private RouterCache<TagDynamicStateRouterTest> buildParsedRuleRouterCache(TagDynamicStateRouter router) {

        String tagRouterRuleConfig = "---\n" +
            "force: false\n" +
            "runtime: true\n" +
            "enabled: true\n" +
            "priority: 1\n" +
            "key: demo-provider\n" +
            "tags:\n" +
            "  - name: tag1\n" +
            "    addresses: [\"30.5.120.37:7777\"]\n" +
            "  - name: tag2\n" +
            "    addresses: [\"30.5.120.37:8888\"]\n" +
            "...";

        Mockito.when(invoker1.getUrl()).thenReturn(URL.valueOf("test://30.5.120.37:7777/test"));
        Mockito.when(invoker2.getUrl()).thenReturn(URL.valueOf("test://30.5.120.37:8888/test"));
        Mockito.when(invoker3.getUrl()).thenReturn(URL.valueOf("test://30.5.120.37:9999/test"));

        ConfigChangedEvent event = Mockito.mock(ConfigChangedEvent.class);
        Mockito.when(event.getContent()).thenReturn(tagRouterRuleConfig);
        Mockito.when(event.getChangeType()).thenReturn(ConfigChangeType.MODIFIED);
        router.process(event);
        return router.pool(invokerList);
    }

}
