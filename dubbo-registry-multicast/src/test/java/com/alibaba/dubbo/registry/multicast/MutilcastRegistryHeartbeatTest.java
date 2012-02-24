/*
 * Copyright 1999-2012 Alibaba Group.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.alibaba.dubbo.registry.multicast;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class MutilcastRegistryHeartbeatTest {

    String service    = "com.alibaba.dubbo.test.injvmServie";

    URL    serviceUrl = URL.valueOf("dubbo://" + NetUtils.getLocalHost() + "/" + service
                                            + "?methods=test1,test2");

    @Test
    public void testHeartbeat() throws Exception {
        URL url = URL.valueOf("multicast://224.10.10.10").addParameter(Constants.HEARTBEAT_KEY, 1000);
        final MulticastRegistry consumerRegistry = new MulticastRegistry(url);
        MulticastRegistry providerRegistry = new MulticastRegistry(
                url.removeParameter(Constants.HEARTBEAT_KEY)
                        .addParameter(Constants.HEARTBEAT_KEY, 2000));
        consumerRegistry.subscribe(serviceUrl, new NotifyListener() {

            public void notify(List<URL> urls) {
                assertEquals(1, consumerRegistry.heartbeatStat.size());
            }
        });
        providerRegistry.register(serviceUrl, null);
        providerRegistry.destroy();
        Thread.sleep(5000);
        assertEquals(0, consumerRegistry.heartbeatStat.size());
        consumerRegistry.destroy();
    }

}
