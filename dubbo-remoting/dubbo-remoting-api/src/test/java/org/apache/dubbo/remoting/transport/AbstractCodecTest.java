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
import org.junit.jupiter.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.mockito.internal.verification.VerificationModeFactory;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AbstractCodecTest  {

    public void test_checkPayload_default8M() throws Exception {
        Channel channel = mock(Channel.class);
        given(channel.getUrl()).willReturn(URL.valueOf("dubbo://1.1.1.1"));

        AbstractCodec.checkPayload(channel, 1 * 1024 * 1024);

        try {
            AbstractCodec.checkPayload(channel, 15 * 1024 * 1024);
        } catch (IOException expected) {
            assertThat(expected.getMessage(), allOf(
                    CoreMatchers.containsString("Data length too large: "),
                    CoreMatchers.containsString("max payload: " + 8 * 1024 * 1024)
            ));
        }

        verify(channel, VerificationModeFactory.atLeastOnce()).getUrl();
    }

    public void test_checkPayload_minusPayloadNoLimit() throws Exception {
        Channel channel = mock(Channel.class);
        given(channel.getUrl()).willReturn(URL.valueOf("dubbo://1.1.1.1?payload=-1"));

        AbstractCodec.checkPayload(channel, 15 * 1024 * 1024);

        verify(channel, VerificationModeFactory.atLeastOnce()).getUrl();
    }
}
