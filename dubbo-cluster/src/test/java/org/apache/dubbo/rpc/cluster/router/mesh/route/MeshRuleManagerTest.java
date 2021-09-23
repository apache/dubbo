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

package org.apache.dubbo.rpc.cluster.router.mesh.route;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.VsDestinationGroup;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MeshRuleManagerTest {

    @Test
    public void subscribeAppRule() {
        Optional<DynamicConfiguration> before = ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().getDynamicConfiguration();
        try {
            DynamicConfiguration dynamicConfiguration = mock(DynamicConfiguration.class);

            ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setDynamicConfiguration(dynamicConfiguration);

            MeshRuleManager.subscribeAppRule(URL.valueOf(""), "test");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(dynamicConfiguration).getConfig(captor.capture(), anyString(), anyLong());

            String result = captor.getValue();

            assertEquals("test.MESHAPPRULE", result);
        } finally {
            ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setDynamicConfiguration(before.orElse(null));
        }


    }

    @Test
    public void register() {
        Optional<DynamicConfiguration> before = ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().getDynamicConfiguration();
        try {
            DynamicConfiguration dynamicConfiguration = mock(DynamicConfiguration.class);

            ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setDynamicConfiguration(dynamicConfiguration);

            when(dynamicConfiguration.getConfig(anyString(), anyString(), anyLong())).thenReturn("apiVersion: service.dubbo.apache.org/v1alpha1\n" +
                    "kind: VirtualService\n" +
                    "metadata: {name: demo-route}\n" +
                    "spec:\n" +
                    "  dubbo:\n" +
                    "    - routedetail:\n" +
                    "        - match:\n" +
                    "            - sourceLabels: {trafficLabel: xxx}\n" +
                    "          name: xxx-project\n" +
                    "          route:\n" +
                    "            - destination: {host: demo, subset: isolation}\n" +
                    "        - match:\n" +
                    "            - sourceLabels: {trafficLabel: testing-trunk}\n" +
                    "          name: testing-trunk\n" +
                    "          route:\n" +
                    "            - destination: {host: demo, subset: testing-trunk}\n" +
                    "        - name: testing\n" +
                    "          route:\n" +
                    "            - destination: {host: demo, subset: testing}\n" +
                    "      services:\n" +
                    "        - {regex: ccc}\n" +
                    "  hosts: [demo]\n");

            MeshRuleManager.subscribeAppRule(URL.valueOf(""), "test");


            MeshRuleRouter meshRuleRouter = mock(MeshRuleRouter.class);

            MeshRuleManager.register("test", meshRuleRouter);

            ArgumentCaptor<VsDestinationGroup> captor = ArgumentCaptor.forClass(VsDestinationGroup.class);


            Mockito.verify(meshRuleRouter).onRuleChange(captor.capture());

            VsDestinationGroup result = captor.getValue();

            assertNotNull(result);
            assertEquals("test", result.getAppName());
            assertEquals(1, result.getVirtualServiceRuleList().size());
            assertEquals(0, result.getDestinationRuleList().size());
        } finally {
            ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setDynamicConfiguration(before.orElse(null));
        }
    }

    @Test
    public void unregister() {
        Optional<DynamicConfiguration> before = ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().getDynamicConfiguration();
        try {
            DynamicConfiguration dynamicConfiguration = mock(DynamicConfiguration.class);

            ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setDynamicConfiguration(dynamicConfiguration);

            when(dynamicConfiguration.getConfig(anyString(), anyString(), anyLong())).thenReturn("apiVersion: service.dubbo.apache.org/v1alpha1\n" +
                    "kind: VirtualService\n" +
                    "metadata: {name: demo-route}\n" +
                    "spec:\n" +
                    "  dubbo:\n" +
                    "    - routedetail:\n" +
                    "        - match:\n" +
                    "            - sourceLabels: {trafficLabel: xxx}\n" +
                    "          name: xxx-project\n" +
                    "          route:\n" +
                    "            - destination: {host: demo, subset: isolation}\n" +
                    "        - match:\n" +
                    "            - sourceLabels: {trafficLabel: testing-trunk}\n" +
                    "          name: testing-trunk\n" +
                    "          route:\n" +
                    "            - destination: {host: demo, subset: testing-trunk}\n" +
                    "        - name: testing\n" +
                    "          route:\n" +
                    "            - destination: {host: demo, subset: testing}\n" +
                    "      services:\n" +
                    "        - {regex: ccc}\n" +
                    "  hosts: [demo]\n");

            MeshRuleManager.subscribeAppRule(URL.valueOf(""), "test");


            MeshRuleRouter meshRuleRouter = mock(MeshRuleRouter.class);

            MeshRuleManager.register("test", meshRuleRouter);

            MeshRuleManager.unregister(meshRuleRouter);

        } finally {
            ApplicationModel.defaultModel().getDefaultModule().getModelEnvironment().setDynamicConfiguration(before.orElse(null));
        }
    }
}
