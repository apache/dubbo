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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.cluster.router.mesh.route.MeshAppRuleListener;
import org.apache.dubbo.rpc.cluster.router.mesh.route.MeshEnvListener;

import com.google.gson.Gson;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KubernetesMeshEnvListener implements MeshEnvListener {
    public static final Logger logger = LoggerFactory.getLogger(KubernetesMeshEnvListener.class);
    private volatile static boolean usingApiServer = false;
    private volatile static KubernetesClient kubernetesClient;
    private volatile static String namespace;

    private final Map<String, MeshAppRuleListener> appRuleListenerMap = new ConcurrentHashMap<>();

    private final Map<String, Watch> vsAppWatch = new ConcurrentHashMap<>();
    private final Map<String, Watch> drAppWatch = new ConcurrentHashMap<>();

    private final Map<String, String> vsAppCache = new ConcurrentHashMap<>();
    private final Map<String, String> drAppCache = new ConcurrentHashMap<>();

    public static void injectKubernetesEnv(KubernetesClient client, String configuredNamespace) {
        usingApiServer = true;
        kubernetesClient = client;
        namespace = configuredNamespace;
    }

    @Override
    public boolean isEnable() {
        return usingApiServer;
    }

    @Override
    public void onSubscribe(String appName, MeshAppRuleListener listener) {
        appRuleListenerMap.put(appName, listener);
        logger.info("Subscribe Mesh Rule in Kubernetes. AppName: " + appName);

        // subscribe VisualService
        subscribeVs(appName);

        // subscribe DestinationRule
        subscribeDr(appName);

        // notify for start
        notifyOnce(appName);
    }

    private void subscribeVs(String appName) {
        if (vsAppWatch.containsKey(appName)) {
            return;
        }

        try {
            Watch watch = kubernetesClient
                .customResource(
                    MeshConstant.getVsDefinition())
                .watch(namespace, appName, null, new ListOptionsBuilder().build(), new Watcher<String>() {
                    @Override
                    public void eventReceived(Action action, String resource) {
                        logger.info("Received VS Rule notification. AppName: " + appName + " Action:" + action + " Resource:" + resource);

                        if (action == Action.ADDED || action == Action.MODIFIED) {
                            Map drRuleMap = new Gson().fromJson(resource, Map.class);
                            String vsRule = new Yaml(new SafeConstructor()).dump(drRuleMap);
                            vsAppCache.put(appName, vsRule);
                            if (drAppCache.containsKey(appName)) {
                                notifyListener(vsRule, appName, drAppCache.get(appName));
                            }
                        } else {
                            appRuleListenerMap.get(appName).receiveConfigInfo("");
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        // ignore
                    }
                });
            vsAppWatch.put(appName, watch);
            try {
                Map<String, Object> vsRule = kubernetesClient
                    .customResource(
                        MeshConstant.getVsDefinition())
                    .get(namespace, appName);
                vsAppCache.put(appName, new Yaml(new SafeConstructor()).dump(vsRule));
            } catch (Throwable ignore) {

            }
        } catch (IOException e) {
            logger.error("Error occurred when listen kubernetes crd.", e);
        }
    }

    private void notifyListener(String vsRule, String appName, String drRule) {
        String rule = vsRule + "\n---\n" + drRule;
        logger.info("Notify App Rule Listener. AppName: " + appName + " Rule:" + rule);

        appRuleListenerMap.get(appName).receiveConfigInfo(rule);
    }

    private void subscribeDr(String appName) {
        if (drAppWatch.containsKey(appName)) {
            return;
        }

        try {
            Watch watch = kubernetesClient
                .customResource(
                    MeshConstant.getDrDefinition())
                .watch(namespace, appName, null, new ListOptionsBuilder().build(), new Watcher<String>() {
                    @Override
                    public void eventReceived(Action action, String resource) {
                        logger.info("Received VS Rule notification. AppName: " + appName + " Action:" + action + " Resource:" + resource);

                        if (action == Action.ADDED || action == Action.MODIFIED) {
                            Map drRuleMap = new Gson().fromJson(resource, Map.class);
                            String drRule = new Yaml(new SafeConstructor()).dump(drRuleMap);

                            drAppCache.put(appName, drRule);
                            if (vsAppCache.containsKey(appName)) {
                                notifyListener(vsAppCache.get(appName), appName, drRule);
                            }
                        } else {
                            appRuleListenerMap.get(appName).receiveConfigInfo("");
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        // ignore
                    }
                });
            drAppWatch.put(appName, watch);
            try {
                Map<String, Object> drRule = kubernetesClient
                    .customResource(
                        MeshConstant.getDrDefinition())
                    .get(namespace, appName);
                drAppCache.put(appName, new Yaml(new SafeConstructor()).dump(drRule));
            } catch (Throwable ignore) {

            }
        } catch (IOException e) {
            logger.error("Error occurred when listen kubernetes crd.", e);
        }
    }

    private void notifyOnce(String appName) {
        if (vsAppCache.containsKey(appName) && drAppCache.containsKey(appName)) {
            notifyListener(vsAppCache.get(appName), appName, drAppCache.get(appName));
        }
    }

    @Override
    public void onUnSubscribe(String appName) {
        appRuleListenerMap.remove(appName);

        if (vsAppWatch.containsKey(appName)) {
            vsAppWatch.remove(appName).close();
        }
        vsAppCache.remove(appName);

        if (drAppWatch.containsKey(appName)) {
            drAppWatch.remove(appName).close();
        }
        drAppCache.remove(appName);
    }
}
