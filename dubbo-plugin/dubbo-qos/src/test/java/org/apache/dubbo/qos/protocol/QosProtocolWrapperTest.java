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
package org.apache.dubbo.qos.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.server.Server;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QosProtocolWrapperTest {
    private URL url = Mockito.mock(URL.class);
    private Invoker invoker = mock(Invoker.class);
    private Protocol protocol = mock(Protocol.class);
    private QosProtocolWrapper wrapper;

    private URL triUrl = Mockito.mock(URL.class);
    private Invoker triInvoker = mock(Invoker.class);
    private Protocol triProtocol = mock(Protocol.class);
    private QosProtocolWrapper triWrapper;

    private Server server;

    @BeforeEach
    public void setUp() throws Exception {
        when(url.getParameter(QOS_ENABLE, true)).thenReturn(true);
        when(url.getParameter(QOS_HOST)).thenReturn("localhost");
        when(url.getParameter(QOS_PORT, 22222)).thenReturn(12345);
        when(url.getParameter(ACCEPT_FOREIGN_IP, "false")).thenReturn("false");
        when(url.getProtocol()).thenReturn(REGISTRY_PROTOCOL);
        when(invoker.getUrl()).thenReturn(url);

        wrapper = new QosProtocolWrapper(protocol);
        wrapper.setFrameworkModel(FrameworkModel.defaultModel());

        // url2 use tri protocol and qos.accept.foreign.ip=true
        when(triUrl.getParameter(QOS_ENABLE, true)).thenReturn(true);
        when(triUrl.getParameter(QOS_HOST)).thenReturn("localhost");
        when(triUrl.getParameter(QOS_PORT, 22222)).thenReturn(12345);
        when(triUrl.getParameter(ACCEPT_FOREIGN_IP, "false")).thenReturn("true");
        when(triUrl.getProtocol()).thenReturn(CommonConstants.TRIPLE);
        when(triInvoker.getUrl()).thenReturn(triUrl);

        triWrapper = new QosProtocolWrapper(triProtocol);
        triWrapper.setFrameworkModel(FrameworkModel.defaultModel());

        server = FrameworkModel.defaultModel().getBeanFactory().getBean(Server.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stop();
        }
        FrameworkModel.defaultModel().destroy();
    }

    @Test
    void testExport() throws Exception {
        wrapper.export(invoker);
        assertThat(server.isStarted(), is(true));
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getPort(), is(12345));
        assertThat(server.isAcceptForeignIp(), is(false));
        verify(protocol).export(invoker);
    }

    @Test
    void testRefer() throws Exception {
        wrapper.refer(BaseCommand.class, url);
        assertThat(server.isStarted(), is(true));
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getPort(), is(12345));
        assertThat(server.isAcceptForeignIp(), is(false));
        verify(protocol).refer(BaseCommand.class, url);
    }

    @Test
    void testMultiProtocol() throws Exception {
        //tri protocol start first, acceptForeignIp = true
        triWrapper.export(triInvoker);
        assertThat(server.isStarted(), is(true));
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getPort(), is(12345));
        assertThat(server.isAcceptForeignIp(), is(true));
        verify(triProtocol).export(triInvoker);

        //next registry protocol server still acceptForeignIp=true even though wrapper invoker url set false
        wrapper.export(invoker);
        assertThat(server.isStarted(), is(true));
        assertThat(server.getHost(), is("localhost"));
        assertThat(server.getPort(), is(12345));
        assertThat(server.isAcceptForeignIp(), is(true));
        verify(protocol).export(invoker);
    }
}
