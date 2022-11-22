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
package org.apache.dubbo.remoting.exchange;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.support.ExchangeHandlerDispatcher;
import org.apache.dubbo.remoting.exchange.support.Replier;
import org.apache.dubbo.remoting.transport.ChannelHandlerAdapter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ExchangersTest {

    @Test
    public void testBind() throws RemotingException {
        String url = "dubbo://127.0.0.1:12345?exchanger=mockExchanger";
        Exchangers.bind(url, Mockito.mock(Replier.class));
        Exchangers.bind(url, new ChannelHandlerAdapter(), Mockito.mock(Replier.class));
        Exchangers.bind(url, new ExchangeHandlerDispatcher());

        Assertions.assertThrows(RuntimeException.class,
                () -> Exchangers.bind((URL) null, new ExchangeHandlerDispatcher()));
        Assertions.assertThrows(RuntimeException.class,
                () -> Exchangers.bind(url, (ExchangeHandlerDispatcher) null));
    }

    @Test
    public void testConnect() throws RemotingException {
        String url = "dubbo://127.0.0.1:12345?exchanger=mockExchanger";
        Exchangers.connect(url);
        Exchangers.connect(url, Mockito.mock(Replier.class));
        Exchangers.connect(URL.valueOf(url), Mockito.mock(Replier.class));
        Exchangers.connect(url, new ChannelHandlerAdapter(), Mockito.mock(Replier.class));
        Exchangers.connect(url, new ExchangeHandlerDispatcher());

        Assertions.assertThrows(RuntimeException.class,
                () -> Exchangers.connect((URL) null, new ExchangeHandlerDispatcher()));
        Assertions.assertThrows(RuntimeException.class,
                () -> Exchangers.connect(url, (ExchangeHandlerDispatcher) null));
    }
}
