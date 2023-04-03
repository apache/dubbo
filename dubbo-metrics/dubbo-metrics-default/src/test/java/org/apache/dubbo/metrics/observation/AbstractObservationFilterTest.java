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

package org.apache.dubbo.metrics.observation;

import io.micrometer.tracing.test.SampleTestRunner;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterEach;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

abstract class AbstractObservationFilterTest extends SampleTestRunner {

    ApplicationModel applicationModel;
    RpcInvocation invocation;

    BaseFilter filter;

    Invoker<?> invoker = mock(Invoker.class);

    static final String INTERFACE_NAME = "org.apache.dubbo.MockInterface";
    static final String METHOD_NAME = "mockMethod";
    static final String GROUP = "mockGroup";
    static final String VERSION = "1.0.0";

    @AfterEach
    public void teardown() {
        if (applicationModel != null) {
            applicationModel.destroy();
        }
    }

    abstract BaseFilter createFilter(ApplicationModel applicationModel);

    void setupConfig() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockObservations");

        applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(config);

        invocation = new RpcInvocation(new MockInvocation());
        invocation.addInvokedInvoker(invoker);

        applicationModel.getBeanFactory().registerBean(getObservationRegistry());
        TracingConfig tracingConfig = new TracingConfig();
        tracingConfig.setEnabled(true);
        applicationModel.getApplicationConfigManager().setTracing(tracingConfig);

        filter = createFilter(applicationModel);

        given(invoker.invoke(invocation)).willReturn(new AppResponse("success"));

        initParam();
    }

    private void initParam() {
        invocation.setTargetServiceUniqueName(GROUP + "/" + INTERFACE_NAME + ":" + VERSION);
        invocation.setMethodName(METHOD_NAME);
        invocation.setParameterTypes(new Class[] {String.class});
    }

}
