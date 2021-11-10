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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.cluster.router.mesh.util.MeshRuleDispatcher;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class MeshAppRuleListener implements ConfigurationListener {

    public static final Logger logger = LoggerFactory.getLogger(MeshAppRuleListener.class);

    public static final String NAME_KEY = "name";

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

            Representer representer = new Representer();
            representer.getPropertyUtils().setSkipMissingProperties(true);
            Yaml yaml = new Yaml(new SafeConstructor(), representer);
            Iterable<Object> yamlIterator = yaml.loadAll(configInfo);

            for (Object obj : yamlIterator) {
                if (obj instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) obj;

                    String ruleType = computeRuleType(resultMap);
                    if (ruleType != null) {
                        groupMap.computeIfAbsent(ruleType, (k)-> new LinkedList<>()).add(resultMap);
                    } else {
                        logger.error("");
                    }
                } else {
                    logger.error("");
                }
            }

            ruleMapHolder = groupMap;
        } catch (Exception e) {
            logger.error("[MeshAppRule] parse failed: " + configInfo, e);
        }
        if (ruleMapHolder != null) {
            meshRuleDispatcher.post(ruleMapHolder);
        }
    }

    @SuppressWarnings("unchecked")
    private String computeRuleType(Map<String, Object> rule) {
        Object obj = rule.get("metadata");
        if (obj instanceof Map && CollectionUtils.isNotEmptyMap((Map<String, String>) obj)) {
            Map<String, String> metadata = (Map<String, String>) obj;
            String name = metadata.get(NAME_KEY);
            // TODO exact constant
            if (name.equals(appName)) {
                return "standard";
            } else if (name.startsWith(appName + ".")) {
                return name.substring(appName.length() + 1);
            }
        }
        return null;
    }

    public void register(MeshRuleRouter subscriber) {
        if (ruleMapHolder != null) {
            List<Map<String, Object>> rule = ruleMapHolder.get(subscriber.ruleSuffix());
            if (rule != null) {
                subscriber.onRuleChange(appName, rule);
            }
        }
        meshRuleDispatcher.register(subscriber);
    }


    public void unregister(MeshRuleRouter sub) {
        meshRuleDispatcher.unregister(sub);
    }

    @Override
    public void process(ConfigChangedEvent event) {
        if (event.getChangeType() == ConfigChangeType.DELETED) {
            receiveConfigInfo("");
            return;
        }
        receiveConfigInfo(event.getContent());
    }
}
