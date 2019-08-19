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
package org.apache.dubbo.rpc.cluster.router;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.dubbo.rpc.cluster.router.tag.model.TagRouterRule;
import org.apache.dubbo.rpc.cluster.router.tag.model.TagRuleParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("FIXME This is not a formal UT")
public class TagRouterTest {
    private static CuratorFramework client;

    @BeforeEach
    public void init() {
        client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", 60 * 1000, 60 * 1000,
                new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    @Test
    public void normalTagRuleTest() {
        String serviceStr = "---\n" +
                "force: false\n" +
                "runtime: true\n" +
                "enabled: false\n" +
                "priority: 1\n" +
                "key: demo-provider\n" +
                "tags:\n" +
                "  - name: tag1\n" +
                "    addresses: [\"30.5.120.37:20881\"]\n" +
                "  - name: tag2\n" +
                "    addresses: [\"30.5.120.37:20880\"]\n" +
                "...";
//        String serviceStr = "";
        try {
            String servicePath = "/dubbo/config/demo-provider/tag-router";
            if (client.checkExists().forPath(servicePath) == null) {
                client.create().creatingParentsIfNeeded().forPath(servicePath);
            }
            setData(servicePath, serviceStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setData(String path, String data) throws Exception {
        client.setData().forPath(path, data.getBytes());
    }

    /**
     * TagRouterRule parse test when the tags addresses is null
     *
     * <pre>
     *     ~ -> null
     *     null -> null
     * </pre>
     */
    @Test
    public void tagRouterRuleParseTest(){
        String tagRouterRuleConfig = "---\n" +
                "force: false\n" +
                "runtime: true\n" +
                "enabled: false\n" +
                "priority: 1\n" +
                "key: demo-provider\n" +
                "tags:\n" +
                "  - name: tag1\n" +
                "    addresses: null\n" +
                "  - name: tag2\n" +
                "    addresses: [\"30.5.120.37:20880\"]\n" +
                "  - name: tag3\n" +
                "    addresses: []\n" +
                "  - name: tag4\n" +
                "    addresses: ~\n" +
                "...";

        TagRouterRule tagRouterRule = TagRuleParser.parse(tagRouterRuleConfig);

        // assert tags
        assert tagRouterRule.getTagNames().contains("tag1");
        assert tagRouterRule.getTagNames().contains("tag2");
        assert tagRouterRule.getTagNames().contains("tag3");
        assert tagRouterRule.getTagNames().contains("tag4");
        // assert addresses
        assert tagRouterRule.getAddresses().contains("30.5.120.37:20880");
        assert tagRouterRule.getTagnameToAddresses().get("tag1")==null;
        assert tagRouterRule.getTagnameToAddresses().get("tag2").size()==1;
        assert tagRouterRule.getTagnameToAddresses().get("tag3")==null;
        assert tagRouterRule.getTagnameToAddresses().get("tag4")==null;
        assert tagRouterRule.getAddresses().size()==1;
    }
}
