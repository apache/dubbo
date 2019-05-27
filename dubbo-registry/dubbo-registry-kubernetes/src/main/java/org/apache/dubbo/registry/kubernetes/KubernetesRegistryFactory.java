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
package org.apache.dubbo.registry.kubernetes;


import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;

/**
 * registry center implementation for kubernetes
 */
public class KubernetesRegistryFactory extends AbstractRegistryFactory {

    private final static String KUBERNETES_NAMESPACE = "kubernetes.namespace";

    private final static String POD_WITH_LABEL = "kubernetes.label";

    protected String kubernetesNamespace;

    protected String podWithLabel;

    @Override
    public Registry createRegistry(URL url) {

        Map<String, String> meta = url.getParameters();
        if (meta == null) {
            throw new IllegalArgumentException("meta data is empty");
        }
        kubernetesNamespace = meta.getOrDefault(KUBERNETES_NAMESPACE,"default");

        podWithLabel = meta.getOrDefault(POD_WITH_LABEL,meta.get(APPLICATION_KEY));

        KubernetesClient client = new DefaultKubernetesClient();

        return new KubernetesRegistry(client, url,kubernetesNamespace,podWithLabel);
    }
}