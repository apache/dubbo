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
package org.apache.dubbo.xds.security.authz.resolver;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.kubernetes.KubeEnv;
import org.apache.dubbo.xds.security.authz.RequestCredential;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

import java.util.Map;

import com.auth0.jwt.interfaces.DecodedJWT;

@Activate
public class KubernetesCredentialResolver implements CredentialResolver {

    private final KubeEnv kubeEnv;

    public KubernetesCredentialResolver(ApplicationModel applicationModel) {
        this.kubeEnv = applicationModel.getBeanFactory().getBean(KubeEnv.class);
    }

    @Override
    public void appendRequestCredential(URL url, Invocation invocation, RequestCredential requestCredential) {
        DecodedJWT jwt = ((DecodedJWT) requestCredential.get(RequestAuthProperty.DECODED_JWT));
        if (jwt == null) {
            return;
        }
        Map<String, Object> kubeProps = jwt.getClaims().get("kubernetes.io").asMap();
        if (kubeProps == null) {
            return;
        }

        String namespace = (String) kubeProps.get("namespace");
        String podName = null;
        String podId = null;
        String sourceService = null;
        String uid = null;
        Map<String, String> serviceAccount = (Map<String, String>) kubeProps.get("serviceaccount");

        if (serviceAccount != null) {
            sourceService = serviceAccount.get("name");
            uid = serviceAccount.get("uid");
        }
        Map<String, String> pod = (Map<String, String>) kubeProps.get("pod");
        if (pod != null) {
            podName = pod.get("name");
            podId = pod.get("uid");
        }

        requestCredential.add(
                RequestAuthProperty.KUBE_SERVICE_PRINCIPAL,
                kubeEnv.getCluster() + "/ns/" + namespace + "/sa/" + sourceService);
        requestCredential.add(RequestAuthProperty.KUBE_POD_NAME, podName);
        requestCredential.add(RequestAuthProperty.KUBE_POD_ID, podId);
        requestCredential.add(RequestAuthProperty.KUBE_SERVICE_UID, uid);
        requestCredential.add(RequestAuthProperty.KUBE_SOURCE_NAMESPACE, namespace);
        requestCredential.add(RequestAuthProperty.KUBE_SERVICE_NAME, sourceService);
    }
}
