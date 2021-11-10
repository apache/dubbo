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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MeshRuleManager {

    public static final Logger logger = LoggerFactory.getLogger(MeshRuleManager.class);

    private static final String MESH_RULE_DATA_ID_SUFFIX = ".MESHAPPRULE";

    private final ConcurrentHashMap<String, MeshAppRuleListener> APP_RULE_LISTENERS = new ConcurrentHashMap<>();

    private final GovernanceRuleRepository ruleRepository;

    private final Set<MeshEnvListener> envListeners;

    public MeshRuleManager(ModuleModel moduleModel) {
        this.ruleRepository = moduleModel.getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension();
        Set<MeshEnvListenerFactory> envListenerFactories = moduleModel.getExtensionLoader(MeshEnvListenerFactory.class).getSupportedExtensionInstances();
        this.envListeners = envListenerFactories.stream()
            .map(MeshEnvListenerFactory::getListener)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public synchronized void subscribeAppRule(String app) {

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
            logger.error("get MeshRuleManager app rule failed.", throwable);
        }

        ruleRepository.addListener(appRuleDataId, DynamicConfiguration.DEFAULT_GROUP, meshAppRuleListener);

        // Add listener to env ( kubernetes, xDS )
        for (MeshEnvListener envListener : envListeners) {
            if (envListener.isEnable()) {
                envListener.onSubscribe(app, meshAppRuleListener);
            }
        }

        APP_RULE_LISTENERS.put(app, meshAppRuleListener);
    }

    public void unsubscribeAppRule(String app) {
        // demo-app.MESHAPPRULE
        String appRuleDataId = app + MESH_RULE_DATA_ID_SUFFIX;

        MeshAppRuleListener meshAppRuleListener = APP_RULE_LISTENERS.get(app);
        if (meshAppRuleListener == null) {
            return;
        }

        // Remove listener from rule repository ( dynamic configuration )
        ruleRepository.removeListener(appRuleDataId, DynamicConfiguration.DEFAULT_GROUP, meshAppRuleListener);

        // Remove listener from env ( kubernetes, xDS )
        for (MeshEnvListener envListener : envListeners) {
            if (envListener.isEnable()) {
                envListener.onUnSubscribe(app);
            }
        }

    }

    public void register(String app, MeshRuleRouter subscriber) {
        MeshAppRuleListener meshAppRuleListener = APP_RULE_LISTENERS.get(app);
        if (meshAppRuleListener == null) {
            logger.warn("appRuleListener can't find when Router register");
            return;
        }
        meshAppRuleListener.register(subscriber);
    }

    public void unregister(MeshRuleRouter subscriber) {
        Collection<MeshAppRuleListener> listeners = APP_RULE_LISTENERS.values();
        for (MeshAppRuleListener listener : listeners) {
            listener.unregister(subscriber);
        }
    }

}
