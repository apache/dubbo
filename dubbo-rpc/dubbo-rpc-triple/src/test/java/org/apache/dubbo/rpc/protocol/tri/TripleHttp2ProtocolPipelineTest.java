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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.nested.TripleConfig;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.api.pu.ChannelHandlerPretender;
import org.apache.dubbo.remoting.api.pu.ChannelOperator;
import org.apache.dubbo.remoting.http12.HttpVersion;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.h12.TripleProtocolDetector;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleServerConnectionHandler;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

/**
 * Verifies that the server pipeline wires the max-connection-age settings from
 * {@link TripleConfig} into {@link TripleServerConnectionHandler}.
 */
class TripleHttp2ProtocolPipelineTest {

    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;

    @BeforeEach
    void setUp() {
        frameworkModel = FrameworkModel.defaultModel();
        applicationModel = frameworkModel.newApplication();
    }

    @AfterEach
    void tearDown() {
        applicationModel.destroy();
    }

    private List<ChannelHandler> configureServerPipeline(long maxConnectionAge, long maxConnectionAgeGrace) {
        TripleConfig tripleConfig = new TripleConfig();
        tripleConfig.setMaxConnectionAge(maxConnectionAge);
        tripleConfig.setMaxConnectionAgeGrace(maxConnectionAgeGrace);
        ProtocolConfig protocolConfig = new ProtocolConfig(CommonConstants.TRIPLE);
        protocolConfig.setTriple(tripleConfig);
        applicationModel.getApplicationConfigManager().addConfig(protocolConfig);

        URL url = URL.valueOf(CommonConstants.TRIPLE + "://127.0.0.1:50051").setScopeModel(applicationModel);

        TripleHttp2Protocol protocol = new TripleHttp2Protocol();
        protocol.setFrameworkModel(frameworkModel);

        ProtocolDetector.Result detectResult = ProtocolDetector.Result.recognized();
        detectResult.setAttribute(TripleProtocolDetector.HTTP_VERSION, HttpVersion.HTTP2.getVersion());
        ChannelOperator operator = Mockito.mock(ChannelOperator.class);
        when(operator.detectResult()).thenReturn(detectResult);

        List<ChannelHandler> captured = new ArrayList<>();
        Mockito.doAnswer(invocation -> {
                    captured.addAll(invocation.getArgument(0));
                    return null;
                })
                .when(operator)
                .configChannelHandler(Mockito.anyList());

        protocol.configServerProtocolHandler(url, operator);
        return captured;
    }

    private static TripleServerConnectionHandler findConnectionHandler(List<ChannelHandler> handlers) {
        for (ChannelHandler handler : handlers) {
            if (handler instanceof ChannelHandlerPretender) {
                Object real = ((ChannelHandlerPretender) handler).getRealHandler();
                if (real instanceof TripleServerConnectionHandler) {
                    return (TripleServerConnectionHandler) real;
                }
            }
        }
        return null;
    }

    @Test
    void testServerPipelineWiresMaxConnectionAge() throws Exception {
        List<ChannelHandler> handlers = configureServerPipeline(60_000L, 5_000L);
        TripleServerConnectionHandler connectionHandler = findConnectionHandler(handlers);
        Assertions.assertNotNull(connectionHandler, "server pipeline should contain TripleServerConnectionHandler");

        java.lang.reflect.Field ageField = TripleServerConnectionHandler.class.getDeclaredField("maxConnectionAge");
        ageField.setAccessible(true);
        java.lang.reflect.Field graceField =
                TripleServerConnectionHandler.class.getDeclaredField("maxConnectionAgeGrace");
        graceField.setAccessible(true);
        Assertions.assertEquals(60_000L, ageField.getLong(connectionHandler));
        Assertions.assertEquals(5_000L, graceField.getLong(connectionHandler));
    }

    @Test
    void testServerPipelineDisabledByDefault() throws Exception {
        List<ChannelHandler> handlers = configureServerPipeline(-1L, 10_000L);
        TripleServerConnectionHandler connectionHandler = findConnectionHandler(handlers);
        Assertions.assertNotNull(connectionHandler);

        java.lang.reflect.Field ageField = TripleServerConnectionHandler.class.getDeclaredField("maxConnectionAge");
        ageField.setAccessible(true);
        Assertions.assertEquals(-1L, ageField.getLong(connectionHandler));
    }
}
