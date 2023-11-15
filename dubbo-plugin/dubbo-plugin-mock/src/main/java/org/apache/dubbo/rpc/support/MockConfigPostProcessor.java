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
package org.apache.dubbo.rpc.support;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ConfigPostProcessor;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.ServiceConfig;

import static org.apache.dubbo.rpc.Constants.RETURN_PREFIX;
import static org.apache.dubbo.rpc.Constants.THROW_PREFIX;

@Activate
public class MockConfigPostProcessor implements ConfigPostProcessor {
    @Override
    public void postProcessReferConfig(ReferenceConfig referenceConfig) {
        checkMock(referenceConfig.getInterfaceClass(), referenceConfig);
    }

    @Override
    public void postProcessServiceConfig(ServiceConfig serviceConfig) {
        checkMock(serviceConfig.getInterfaceClass(), serviceConfig);
    }

    /**
     * Legitimacy check and setup of local simulated operations. The operations can be a string with Simple operation or
     * a classname whose {@link Class} implements a particular function
     *
     * @param interfaceClass for provider side, it is the {@link Class} of the service that will be exported; for consumer
     *                       side, it is the {@link Class} of the remote service interface that will be referenced
     */
    public static void checkMock(Class<?> interfaceClass, AbstractInterfaceConfig config) {
        String mock = config.getMock();
        if (ConfigUtils.isEmpty(mock)) {
            return;
        }

        String normalizedMock = MockInvoker.normalizeMock(mock);
        if (normalizedMock.startsWith(RETURN_PREFIX)) {
            normalizedMock = normalizedMock.substring(RETURN_PREFIX.length()).trim();
            try {
                // Check whether the mock value is legal, if it is illegal, throw exception
                MockInvoker.parseMockValue(normalizedMock);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Illegal mock return in <dubbo:service/reference ... " + "mock=\"" + mock + "\" />");
            }
        } else if (normalizedMock.startsWith(THROW_PREFIX)) {
            normalizedMock = normalizedMock.substring(THROW_PREFIX.length()).trim();
            if (ConfigUtils.isNotEmpty(normalizedMock)) {
                try {
                    // Check whether the mock value is legal
                    MockInvoker.getThrowable(normalizedMock);
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Illegal mock throw in <dubbo:service/reference ... " + "mock=\"" + mock + "\" />");
                }
            }
        } else {
            // Check whether the mock class is a implementation of the interfaceClass, and if it has a default
            // constructor
            MockInvoker.getMockObject(config.getScopeModel().getExtensionDirector(), normalizedMock, interfaceClass);
        }
    }
}
