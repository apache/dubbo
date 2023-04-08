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
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.cluster.router.mesh.util.MeshRuleDispatcher;
import org.apache.dubbo.rpc.cluster.router.mesh.util.MeshRuleListener;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_RECEIVE_RULE;
import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.METADATA_KEY;
import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.NAME_KEY;
import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.STANDARD_ROUTER_KEY;


public class MeshAppRuleListener implements ConfigurationListener {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MeshAppRuleListener.class);

    private final MeshRuleDispatcher meshRuleDispatcher;

    private final String appName;

    private volatile Map<String, List<Map<String, Object>>> ruleMapHolder;

    public MeshAppRuleListener(String appName) {
        this.appName = appName;
        this.meshRuleDispatcher = new MeshRuleDispatcher(appName);
    }

    @SuppressWarnings("unchecked")
    public void receiveConfigInfo(String configInfo) {
        if (logger.isDebugEnabled()) {
            logger.debug(MessageFormat.format("[MeshAppRule] Received rule for app [{0}]: {1}.",
                appName, configInfo));
        }
        try {
            Map<String, List<Map<String, Object>>> groupMap = new HashMap<>();

            Representer representer = new Representer(new DumperOptions());
            representer.getPropertyUtils().setSkipMissingProperties(true);
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), representer);
            Iterable<Object> yamlIterator = yaml.loadAll(configInfo);

            for (Object obj : yamlIterator) {
                if (obj instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) obj;

                    String ruleType = computeRuleType(resultMap);
                    if (ruleType != null) {
                        groupMap.computeIfAbsent(ruleType, (k)-> new LinkedList<>()).add(resultMap);
                    } else {
                        logger.error(CLUSTER_FAILED_RECEIVE_RULE,"receive mesh app route rule is invalid","","Unable to get rule type from raw rule. " +
                            "Probably the metadata.name is absent. App Name: " + appName + " RawRule: " + configInfo);
                    }
                } else {
                    logger.error(CLUSTER_FAILED_RECEIVE_RULE,"receive mesh app route rule is invalid","","Rule format is unacceptable. App Name: " + appName + " RawRule: " + configInfo);
                }
            }

            ruleMapHolder = groupMap;
        } catch (Exception e) {
            logger.error(CLUSTER_FAILED_RECEIVE_RULE,"failed to receive mesh app route rule","","[MeshAppRule] parse failed: " + configInfo,e);
        }
        if (ruleMapHolder != null) {
            meshRuleDispatcher.post(ruleMapHolder);
        }
    }

    @SuppressWarnings("unchecked")
    private String computeRuleType(Map<String, Object> rule) {
        Object obj = rule.get(METADATA_KEY);
        if (obj instanceof Map && CollectionUtils.isNotEmptyMap((Map<String, String>) obj)) {
            Map<String, String> metadata = (Map<String, String>) obj;
            String name = metadata.get(NAME_KEY);
            if (!name.contains(".")) {
                return STANDARD_ROUTER_KEY;
            } else {
                return name.substring(name.indexOf(".") + 1);
            }
        }
        return null;
    }

    public <T> void register(MeshRuleListener subscriber) {
        if (ruleMapHolder != null) {
            List<Map<String, Object>> rule = ruleMapHolder.get(subscriber.ruleSuffix());
            if (rule != null) {
                subscriber.onRuleChange(appName, rule);
            }
        }
        meshRuleDispatcher.register(subscriber);
    }


    public <T> void unregister(MeshRuleListener subscriber) {
        meshRuleDispatcher.unregister(subscriber);
    }

    @Override
    public void process(ConfigChangedEvent event) {
        if (event.getChangeType() == ConfigChangeType.DELETED) {
            receiveConfigInfo("");
            return;
        }
        receiveConfigInfo(event.getContent());
    }

    public boolean isEmpty() {
        return meshRuleDispatcher.isEmpty();
    }

    /**
     * For ut only
     */
    @Deprecated
    public MeshRuleDispatcher getMeshRuleDispatcher() {
        return meshRuleDispatcher;
    }
}
