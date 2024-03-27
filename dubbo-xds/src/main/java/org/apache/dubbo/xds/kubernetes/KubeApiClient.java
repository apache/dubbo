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
package org.apache.dubbo.xds.kubernetes;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Watch;
import io.kubernetes.client.util.Watch.Response;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;

public class KubeApiClient {
    private final ApiClient apiClient;

    private final ErrorTypeAwareLogger errorTypeAwareLogger =
            LoggerFactory.getErrorTypeAwareLogger(KubeApiClient.class);

    public KubeApiClient(ApplicationModel applicationModel) throws IOException {
        KubeEnv kubeEnv = applicationModel.getBeanFactory().getBean(KubeEnv.class);

        apiClient = new ClientBuilder()
                .setBasePath(kubeEnv.getServiceAccountPath())
                .setVerifyingSsl(kubeEnv.enableSsl())
                .setAuthentication(new AccessTokenAuthentication(kubeEnv.getServiceAccountToken()))
                .build();

        apiClient.setConnectTimeout(kubeEnv.apiClientConnectTimeout());
        apiClient.setReadTimeout(kubeEnv.apiClientReadTimeout());

        Configuration.setDefaultApiClient(apiClient);
    }

    public Map<String, Object> getResourceAsMap(String apiGroup, String version, String namespace, String plural) {
        CustomObjectsApi apiInstance = new CustomObjectsApi();
        try {
            return (Map<String, Object>) apiInstance.listNamespacedCustomObject(
                    apiGroup, version, namespace, plural, null, null, null, null, null, null, null, null);
        } catch (ApiException apiException) {
            // log
            throw new RuntimeException("Failed to get CRDs from ApiServer.", apiException);
        }
    }

    public Watch<Object> listenResource(String apiGroup, String version, String namespace, String plural)
            throws ApiException {
        CustomObjectsApi api = new CustomObjectsApi();
        return Watch.createWatch(
                apiClient,
                api.listNamespacedCustomObjectCall(
                        apiGroup, version, namespace, plural, null, null, null, null, null, null, null, null, null),
                new TypeToken<Response<Object>>() {}.getType());
    }
}
