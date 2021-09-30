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

import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.VsDestinationGroup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class MeshAppRuleListenerTest {

    @Test
    public void receiveConfigInfo() {
        MeshAppRuleListener meshAppRuleListener = new MeshAppRuleListener("qinliujie");

        MeshRuleRouter meshRuleRouter = mock(MeshRuleRouter.class);
        meshAppRuleListener.register(meshRuleRouter);

        meshAppRuleListener.receiveConfigInfo("apiVersion: service.dubbo.apache.org/v1alpha1\n" +
                "kind: DestinationRule\n" +
                "metadata: { name: demo-route }\n" +
                "spec:\n" +
                "  host: demo\n" +
                "  subsets:\n" +
                "    - labels: { env-sign: xxx, tag1: hello }\n" +
                "      name: isolation\n" +
                "    - labels: { env-sign: yyy }\n" +
                "      name: testing-trunk\n" +
                "    - labels: { env-sign: zzz }\n" +
                "      name: testing\n" +
                "  trafficPolicy:\n" +
                "    loadBalancer: { simple: ROUND_ROBIN }\n" +
                "\n" +
                "---\n" +
                "\n" +
                "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
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


        ArgumentCaptor<VsDestinationGroup> captor = ArgumentCaptor.forClass(VsDestinationGroup.class);
        verify(meshRuleRouter, times(1)).onRuleChange(captor.capture());

        VsDestinationGroup vsDestinationGroup = captor.getValue();

        assertTrue(vsDestinationGroup.getAppName().equals("qinliujie"));
        assertTrue(vsDestinationGroup.getDestinationRuleList().size() == 1);
        assertTrue(vsDestinationGroup.getVirtualServiceRuleList().size() == 1);


        meshAppRuleListener.receiveConfigInfo("");
        verify(meshRuleRouter, times(2)).onRuleChange(captor.capture());

        VsDestinationGroup vsDestinationGroup1 = captor.getAllValues().get(captor.getAllValues().size() - 1);

        assertTrue(vsDestinationGroup1.getAppName().equals("qinliujie"));
        assertTrue(vsDestinationGroup1.getDestinationRuleList().size() == 0);
        assertTrue(vsDestinationGroup1.getVirtualServiceRuleList().size() == 0);
    }

    @Test
    public void register() {
    }

    @Test
    public void unregister() {
    }

    @Test
    public void process() {
        MeshAppRuleListener meshAppRuleListener = new MeshAppRuleListener("qinliujie");

        MeshRuleRouter meshRuleRouter = mock(MeshRuleRouter.class);
        meshAppRuleListener.register(meshRuleRouter);

        ConfigChangedEvent configChangedEvent = new ConfigChangedEvent("qinliujie", "HSF", "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
                "kind: DestinationRule\n" +
                "metadata: { name: demo-route }\n" +
                "spec:\n" +
                "  host: demo\n" +
                "  subsets:\n" +
                "    - labels: { env-sign: xxx, tag1: hello }\n" +
                "      name: isolation\n" +
                "    - labels: { env-sign: yyy }\n" +
                "      name: testing-trunk\n" +
                "    - labels: { env-sign: zzz }\n" +
                "      name: testing\n" +
                "  trafficPolicy:\n" +
                "    loadBalancer: { simple: ROUND_ROBIN }\n" +
                "\n" +
                "---\n" +
                "\n" +
                "apiVersion: service.dubbo.apache.org/v1alpha1\n" +
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
                "  hosts: [demo]\n", ConfigChangeType.MODIFIED);


        meshAppRuleListener.process(configChangedEvent);

        ArgumentCaptor<VsDestinationGroup> captor = ArgumentCaptor.forClass(VsDestinationGroup.class);
        verify(meshRuleRouter, times(1)).onRuleChange(captor.capture());

        VsDestinationGroup vsDestinationGroup = captor.getValue();

        assertTrue(vsDestinationGroup.getAppName().equals("qinliujie"));
        assertTrue(vsDestinationGroup.getDestinationRuleList().size() == 1);
        assertTrue(vsDestinationGroup.getVirtualServiceRuleList().size() == 1);


        meshAppRuleListener.receiveConfigInfo("");
        verify(meshRuleRouter, times(2)).onRuleChange(captor.capture());

        VsDestinationGroup vsDestinationGroup1 = captor.getAllValues().get(captor.getAllValues().size() - 1);

        assertTrue(vsDestinationGroup1.getAppName().equals("qinliujie"));
        assertTrue(vsDestinationGroup1.getDestinationRuleList().size() == 0);
        assertTrue(vsDestinationGroup1.getVirtualServiceRuleList().size() == 0);

    }
}
