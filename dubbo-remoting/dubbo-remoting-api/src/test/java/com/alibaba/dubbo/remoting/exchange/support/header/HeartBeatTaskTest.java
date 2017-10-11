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

package com.alibaba.dubbo.remoting.exchange.support.header;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.exchange.Request;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class HeartBeatTaskTest {

    private URL url = URL.valueOf("dubbo://localhost:20880");

    private MockChannel channel;
    private HeartBeatTask task;

    @Before
    public void setup() throws Exception {
        task = new HeartBeatTask(new HeartBeatTask.ChannelProvider() {

            public Collection<Channel> getChannels() {
                return Collections.<Channel>singletonList(channel);
            }
        }, 1000, 1000 * 3);

        channel = new MockChannel() {

            @Override
            public URL getUrl() {
                return url;
            }
        };
    }

    @Test
    public void testHeartBeat() throws Exception {
        url = url.addParameter(Constants.DUBBO_VERSION_KEY, "2.1.1");
        channel.setAttribute(
                HeaderExchangeHandler.KEY_READ_TIMESTAMP, System.currentTimeMillis());
        channel.setAttribute(
                HeaderExchangeHandler.KEY_WRITE_TIMESTAMP, System.currentTimeMillis());
        Thread.sleep(2000L);
        task.run();
        List<Object> objects = channel.getSentObjects();
        Assert.assertTrue(objects.size() > 0);
        Object obj = objects.get(0);
        Assert.assertTrue(obj instanceof Request);
        Request request = (Request) obj;
        Assert.assertTrue(request.isHeartbeat());
    }

}
