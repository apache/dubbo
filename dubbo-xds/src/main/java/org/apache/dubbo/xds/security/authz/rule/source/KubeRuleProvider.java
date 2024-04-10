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
package org.apache.dubbo.xds.security.authz.rule.source;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.kubernetes.KubeApiClient;
import org.apache.dubbo.xds.kubernetes.KubeEnv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.Watch;

@Activate
public class KubeRuleProvider implements RuleProvider<Map<String, Object>> {

    protected final KubeApiClient kubeApiClient;

    private volatile List<Map<String, Object>> ruleSourceInst;

    protected KubeEnv kubeEnv;

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(KubeRuleProvider.class);

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            1, task -> new Thread(task, "KubeRuleSourceProvider-Scheduled-AutoRefresh"));

    public KubeRuleProvider(ApplicationModel applicationModel) throws Exception {
        this.kubeApiClient = applicationModel.getBeanFactory().getBean(KubeApiClient.class);
        this.kubeEnv = applicationModel.getBeanFactory().getBean(KubeEnv.class);
        Map<String, Object> resource = getResource();
        updateSource(resource);
        startListenRequestAuthentication();
    }

    @Override
    public List<Map<String, Object>> getSource(URL url, Invocation invocation) {
        return new ArrayList<>(ruleSourceInst);
    }

    private void startListenRequestAuthentication() throws ApiException {

        Watch<Object> watch = getResourceListen();

        executor.scheduleAtFixedRate(
                () -> {
                    try {
                        Map<String, Object> resource = getResource();
                        updateSource(resource);
                        //                        if (watch.hasNext()) {
                        //                            Response<Object> resp = watch.next();
                        //                            if ("ADDED".equals(resp.type) || "MODIFIED".equals(resp.type)) {
                        //                                updateSource((Map<String, Object>) resp.object);
                        //                            } else if ("DELETED".equals(resp.type)) {
                        //                                ruleSourceInst = Collections.emptyList();
                        //                            }
                        //                            System.out.println("resource updated"+ resp.object);
                        //                        }
                    } catch (Exception e) {
                        logger.error(
                                "", "", "", "Got exception when watch and updating RequestAuthorization resource", e);
                    }
                },
                2000,
                2000,
                TimeUnit.MILLISECONDS);
    }

    protected Map<String, Object> getResource() {
        return kubeApiClient.getResourceAsMap(
                "security.istio.io", "v1", kubeEnv.getNamespace(), "authorizationpolicies");
    }

    protected Watch<Object> getResourceListen() {
        return kubeApiClient.listenResource("security.istio.io", "v1", kubeEnv.getNamespace(), "authorizationpolicies");
    }

    protected void updateSource(Map<String, Object> resultMap) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) resultMap.get("items");
        List<Map<String, Object>> rules = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> spec = (Map<String, Object>) item.get("spec");
            boolean match = false;
            if (spec != null) {
                Map<String, Object> selector = (Map<String, Object>) spec.get("selector");
                if (selector != null) {
                    Map<String, String> matchLabels = (Map<String, String>) selector.get("matchLabels");

                    String targetLabelKey = "app";
                    String targetLabelValue = kubeEnv.getServiceName();

                    if (matchLabels != null
                            && (StringUtils.isEmpty(targetLabelValue)
                                    || targetLabelValue.equals(matchLabels.get(targetLabelKey)))) {
                        match = true;
                    }
                } else {
                    // no selector set
                    match = true;
                }
                if (match) {
                    rules.add(spec);
                }
            }
        }
        this.ruleSourceInst = rules;
    }
}
