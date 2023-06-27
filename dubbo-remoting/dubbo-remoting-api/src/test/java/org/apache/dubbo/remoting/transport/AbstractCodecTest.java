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
package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;

import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.VerificationModeFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AbstractCodecTest {

    @Test
    void testCheckPayloadDefault8M() throws Exception {
        Channel channel = mock(Channel.class);
        given(channel.getUrl()).willReturn(URL.valueOf("dubbo://1.1.1.1"));

        AbstractCodec.checkPayload(channel, 1 * 1024 * 1024);

        try {
            AbstractCodec.checkPayload(channel, 15 * 1024 * 1024);
        } catch (IOException expected) {
            assertThat(expected.getMessage(), allOf(
                containsString("Data length too large: "),
                containsString("max payload: " + 8 * 1024 * 1024)
            ));
        }

        verify(channel, VerificationModeFactory.atLeastOnce()).getUrl();
    }

    @Test
    void testCheckProviderPayload() throws Exception {
        Channel channel = mock(Channel.class);
        given(channel.getUrl()).willReturn(URL.valueOf("dubbo://1.1.1.1"));

        AbstractCodec.checkPayload(channel, 1024 * 1024 + 1, 1024 * 1024);

        try {
            AbstractCodec.checkPayload(channel, 1024 * 1024, 1024 * 1024);
        } catch (IOException expected) {
            assertThat(expected.getMessage(), allOf(
                containsString("Data length too large: "),
                containsString("max payload: " + 1024 * 1024)
            ));
        }

        try {
            AbstractCodec.checkPayload(channel, 0, 15 * 1024 * 1024);
        } catch (IOException expected) {
            assertThat(expected.getMessage(), allOf(
                containsString("Data length too large: "),
                containsString("max payload: " + 8 * 1024 * 1024)
            ));
        }

        verify(channel, VerificationModeFactory.atLeastOnce()).getUrl();
    }

    @Test
    void tesCheckPayloadMinusPayloadNoLimit() throws Exception {
        Channel channel = mock(Channel.class);
        given(channel.getUrl()).willReturn(URL.valueOf("dubbo://1.1.1.1?payload=-1"));

        AbstractCodec.checkPayload(channel, 15 * 1024 * 1024);

        verify(channel, VerificationModeFactory.atLeastOnce()).getUrl();
    }

    @Test
    void testIsClientSide() {
        AbstractCodec codec = getAbstractCodec();

        Channel channel = mock(Channel.class);
        given(channel.getRemoteAddress()).willReturn(new InetSocketAddress("172.24.157.13", 9103));
        given(channel.getUrl()).willReturn(URL.valueOf("dubbo://172.24.157.13:9103"));
        assertThat(codec.isClientSide(channel), is(true));
        assertThat(codec.isServerSide(channel), is(false));

        given(channel.getRemoteAddress()).willReturn(new InetSocketAddress("172.24.157.14", 9103));
        given(channel.getUrl()).willReturn(URL.valueOf("dubbo://172.24.157.13:9103"));
        assertThat(codec.isClientSide(channel), is(false));
        assertThat(codec.isServerSide(channel), is(true));

    }

    private AbstractCodec getAbstractCodec() {
        AbstractCodec codec = new AbstractCodec() {
            @Override
            public void encode(Channel channel, ChannelBuffer buffer, Object message) {

            }

            @Override
            public Object decode(Channel channel, ChannelBuffer buffer) {
                return null;
            }
        };
        return codec;
    }
}
