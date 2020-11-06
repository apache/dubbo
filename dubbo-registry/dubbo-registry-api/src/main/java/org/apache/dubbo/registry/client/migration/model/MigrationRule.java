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
package org.apache.dubbo.registry.client.migration.model;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * # key = demo-consumer.migration
 * # group = DUBBO_SERVICEDISCOVERY_MIGRATION
 * # content
 * key: demo-consumer
 * step: APPLICATION_FIRST
 * threshold: 1.0
 * interfaces:
 * - serviceKey: DemoService:1.0.0
 * threshold: 1.0
 * step: APPLICATION_FIRST
 * - serviceKey: GreetingService:1.0.0
 * step: FORCE_APPLICATION
 */
public class MigrationRule {
    private String key;
    private MigrationStep step;
    private String threshold;
    private List<InterfaceMigrationRule> interfaces;

    private transient Map<String, InterfaceMigrationRule> interfaceRules;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public MigrationStep getStep(String serviceKey) {
        InterfaceMigrationRule rule = interfaceRules.get(serviceKey);
        if (rule != null) {
            return rule.getStep() == null ? step : rule.getStep();
        }
        return step;
    }

    public InterfaceMigrationRule getInterfaceRule(String serviceKey) {
        return interfaceRules.get(serviceKey);
    }

    public MigrationStep getStep() {
        return step;
    }

    public String getThreshold(String serviceKey) {
        InterfaceMigrationRule rule = interfaceRules.get(serviceKey);
        if (rule != null) {
            return rule.getThreshold() == null ? threshold : rule.getThreshold();
        }
        return threshold;
    }

    public String getThreshold() {
        return threshold;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }

    public void setStep(MigrationStep step) {
        this.step = step;
    }

    public List<InterfaceMigrationRule> getInterfaces() {
        return interfaces;
    }


    public void setInterfaces(List<InterfaceMigrationRule> interfaces) {
        this.interfaces = interfaces;
        if (interfaces != null) {
            this.interfaceRules = new HashMap<>();
            interfaces.forEach(rule -> {
                interfaceRules.put(rule.getServiceKey(), rule);
            });
        }
    }

    public static MigrationRule parse(String rawRule) {
        Constructor constructor = new Constructor(MigrationRule.class);
        Yaml yaml = new Yaml(constructor);
        return yaml.load(rawRule);
    }
}
