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

import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;
import org.apache.dubbo.rpc.cluster.router.mesh.util.MeshRuleListener;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_RECEIVE_RULE;
import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.MESH_RULE_DATA_ID_SUFFIX;

public class MeshRuleManager {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MeshRuleManager.class);

    private final ConcurrentHashMap<String, MeshAppRuleListener> APP_RULE_LISTENERS = new ConcurrentHashMap<>();

    private final GovernanceRuleRepository ruleRepository;

    private final Set<MeshEnvListener> envListeners;

    public MeshRuleManager(ModuleModel moduleModel) {
        this.ruleRepository = moduleModel.getDefaultExtension(GovernanceRuleRepository.class);
        Set<MeshEnvListenerFactory> envListenerFactories = moduleModel.getExtensionLoader(MeshEnvListenerFactory.class).getSupportedExtensionInstances();
        this.envListeners = envListenerFactories.stream()
            .map(MeshEnvListenerFactory::getListener)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private synchronized MeshAppRuleListener subscribeAppRule(String app) {

        MeshAppRuleListener meshAppRuleListener = new MeshAppRuleListener(app);
        // demo-app.MESHAPPRULE
        String appRuleDataId = app + MESH_RULE_DATA_ID_SUFFIX;

        // Add listener to rule repository ( dynamic configuration )
        try {
            String rawConfig = ruleRepository.getRule(appRuleDataId, DynamicConfiguration.DEFAULT_GROUP, 5000L);
            if (rawConfig != null) {
                meshAppRuleListener.receiveConfigInfo(rawConfig);
            }
        } catch (Throwable throwable) {
            logger.error(CLUSTER_FAILED_RECEIVE_RULE,"failed to get mesh app route rule","","get MeshRuleManager app rule failed.",throwable);
        }

        ruleRepository.addListener(appRuleDataId, DynamicConfiguration.DEFAULT_GROUP, meshAppRuleListener);

        // Add listener to env ( kubernetes, xDS )
        for (MeshEnvListener envListener : envListeners) {
            if (envListener.isEnable()) {
                envListener.onSubscribe(app, meshAppRuleListener);
            }
        }

        APP_RULE_LISTENERS.put(app, meshAppRuleListener);
        return meshAppRuleListener;
    }

    private synchronized void unsubscribeAppRule(String app, MeshAppRuleListener meshAppRuleListener) {
        // demo-app.MESHAPPRULE
        String appRuleDataId = app + MESH_RULE_DATA_ID_SUFFIX;

        // Remove listener from rule repository ( dynamic configuration )
        ruleRepository.removeListener(appRuleDataId, DynamicConfiguration.DEFAULT_GROUP, meshAppRuleListener);

        // Remove listener from env ( kubernetes, xDS )
        for (MeshEnvListener envListener : envListeners) {
            if (envListener.isEnable()) {
                envListener.onUnSubscribe(app);
            }
        }

    }

    public synchronized <T> void register(String app, MeshRuleListener subscriber) {
        MeshAppRuleListener meshAppRuleListener = APP_RULE_LISTENERS.get(app);
        if (meshAppRuleListener == null) {
            meshAppRuleListener = subscribeAppRule(app);
        }
        meshAppRuleListener.register(subscriber);
    }

    public synchronized <T> void unregister(String app, MeshRuleListener subscriber) {
        MeshAppRuleListener meshAppRuleListener = APP_RULE_LISTENERS.get(app);
        meshAppRuleListener.unregister(subscriber);
        if (meshAppRuleListener.isEmpty()) {
            unsubscribeAppRule(app, meshAppRuleListener);
            APP_RULE_LISTENERS.remove(app);
        }
    }

    /**
     * for ut only
     */
    @Deprecated
    public ConcurrentHashMap<String, MeshAppRuleListener> getAppRuleListeners() {
        return APP_RULE_LISTENERS;
    }
}
