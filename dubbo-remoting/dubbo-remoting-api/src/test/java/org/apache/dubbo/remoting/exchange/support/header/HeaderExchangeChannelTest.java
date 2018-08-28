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

package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.ResponseFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class HeaderExchangeChannelTest {
    private MockChannel channel;
    private HeaderExchangeChannel headerExchangeChannel;
    private HeaderExchangeChannel closedHeaderExchangeChannel;
    private URL url = URL.valueOf("dubbo://localhost:20880");

    private MockChannel makeChannel() {
        return new MockChannel() {
            @Override
            public URL getUrl() {
                return url;
            }
        };
    }

    @Before
    public void setup() throws Exception {
        channel = this.makeChannel();
        headerExchangeChannel = new HeaderExchangeChannel(channel);

        closedHeaderExchangeChannel = new HeaderExchangeChannel(this.makeChannel());
        closedHeaderExchangeChannel.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructWithNullChannel() {
        new HeaderExchangeChannel(null);
    }

    @Test
    public void testGetOrAddChannel() {
        Assert.assertNull(HeaderExchangeChannel.getOrAddChannel(null));

        HeaderExchangeChannel result = HeaderExchangeChannel.getOrAddChannel(channel);
        Assert.assertEquals(headerExchangeChannel, result);
    }

    @Test
    public void testSendString() throws Exception {
        final String requestData = "charles";

        headerExchangeChannel.send(requestData);
        List<Object> objects = channel.getSentObjects();
        Assert.assertTrue(objects.size() > 0);

        Object obj = objects.get(0);
        Assert.assertTrue(obj instanceof String);

        String data = (String) obj;
        Assert.assertEquals(requestData, data);
    }

    @Test
    public void testSendRequest() throws Exception {
        final Request req = new Request();

        headerExchangeChannel.send(req);
        List<Object> objects = channel.getSentObjects();
        Assert.assertTrue(objects.size() > 0);

        Object obj = objects.get(0);
        Assert.assertTrue(obj instanceof Request);

        Request request = (Request) obj;
        Assert.assertEquals(req, request);
    }

    @Test
    public void testSendObject() throws Exception {
        final Person requestData = new Person("charles");

        headerExchangeChannel.send(requestData);
        List<Object> objects = channel.getSentObjects();
        Assert.assertTrue(objects.size() > 0);

        Object obj = objects.get(0);
        Assert.assertTrue(obj instanceof Request);

        Request request = (Request) obj;
        Assert.assertFalse(request.isTwoWay());

        Object data = request.getData();
        Assert.assertEquals(requestData, data);
    }

    @Test(expected = RemotingException.class)
    public void testSendWithClosedChannel() throws Exception {
        closedHeaderExchangeChannel.send("Charles");
    }

    @Test(expected = TimeoutException.class)
    public void testRequest() throws Exception {
        final Person requestData = new Person("charles");
        final int timeout = 200;

        ResponseFuture future = headerExchangeChannel.request(requestData, timeout);

        List<Object> objects = channel.getSentObjects();
        Assert.assertTrue(objects.size() > 0);

        Object obj = objects.get(0);
        Assert.assertTrue(obj instanceof Request);

        Request request = (Request) obj;
        Assert.assertTrue(request.isTwoWay());

        Object data = request.getData();
        Assert.assertEquals(requestData, data);

        future.get();
    }

    @Test(expected = RemotingException.class)
    public void testRequestWithClosedChannel() throws Exception {
        closedHeaderExchangeChannel.request("Charles");
    }

    @Test
    public void testHashCode() throws Exception {
        int hashCode = headerExchangeChannel.hashCode();
        Assert.assertEquals(hashCode, headerExchangeChannel.hashCode());

        Channel anotherChannel = new MockChannel();
        HeaderExchangeChannel anotherHeaderExchangeChannel = new HeaderExchangeChannel(anotherChannel);
        Assert.assertNotEquals(hashCode, anotherHeaderExchangeChannel.hashCode());
    }

    @Test
    public void testEquals() throws Exception {
        Assert.assertEquals(headerExchangeChannel, headerExchangeChannel);
        Assert.assertNotEquals(headerExchangeChannel, null);
        Assert.assertNotEquals(headerExchangeChannel, new Person("charles"));

        Channel anotherChannel = new MockChannel();
        Assert.assertNotEquals(headerExchangeChannel, new HeaderExchangeChannel(anotherChannel));

        Assert.assertEquals(headerExchangeChannel, new HeaderExchangeChannel(channel));
    }

    @After
    public void tearDown() throws Exception {
        headerExchangeChannel.close();
        Assert.assertTrue(channel.isClosed());
    }

    private class Person {
        private String name;

        public Person(String name) {
            super();
            this.name = name;
        }

        @Override
        public String toString() {
            return "Person [name=" + name + "]";
        }
    }
}
