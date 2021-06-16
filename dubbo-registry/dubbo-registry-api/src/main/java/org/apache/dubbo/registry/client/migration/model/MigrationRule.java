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

import org.apache.dubbo.common.utils.CollectionUtils;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * # key = demo-consumer.migration
 * # group = DUBBO_SERVICEDISCOVERY_MIGRATION
 * # content
 * key: demo-consumer
 * step: APPLICATION_FIRST
 * threshold: 1.0
 * proportion: 60
 * delay: 60
 * force: false
 * interfaces:
 *   - serviceKey: DemoService:1.0.0
 *     threshold: 0.5
 *     proportion: 30
 *     delay: 30
 *     force: true
 *     step: APPLICATION_FIRST
 *   - serviceKey: GreetingService:1.0.0
 *     step: FORCE_APPLICATION
 */
public class MigrationRule {
    public static final MigrationRule INIT = new MigrationRule();

    private String key;
    private MigrationStep step;
    private Float threshold;
    private Integer proportion;
    private Integer delay;
    private Boolean force;
    private List<InterfaceMigrationRule> interfaces;

    private transient Map<String, InterfaceMigrationRule> interfaceRules;

    @SuppressWarnings("unchecked")
    private static MigrationRule parseFromMap(Map<String, Object> map) {
        MigrationRule migrationRule = new MigrationRule();
        migrationRule.setKey((String) map.get("key"));

        Object step = map.get("step");
        if (step != null) {
            migrationRule.setStep(MigrationStep.valueOf(step.toString()));
        }

        Object threshold = map.get("threshold");
        if (threshold != null) {
            migrationRule.setThreshold(Float.valueOf(threshold.toString()));
        }

        Object proportion = map.get("proportion");
        if (proportion != null) {
            migrationRule.setProportion(Integer.valueOf(proportion.toString()));
        }

        Object delay = map.get("delay");
        if (delay != null) {
            migrationRule.setDelay(Integer.valueOf(delay.toString()));
        }

        Object force = map.get("force");
        if (force != null) {
            migrationRule.setForce(Boolean.valueOf(force.toString()));
        }

        Object interfaces = map.get("interfaces");
        if (interfaces != null && List.class.isAssignableFrom(interfaces.getClass())) {
            migrationRule.setInterfaces(((List<Map<String, Object>>) interfaces).stream()
                    .map(InterfaceMigrationRule::parseFromMap).collect(Collectors.toList()));
        }

        return migrationRule;
    }

    public MigrationRule() {
    }

    public MigrationRule(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public MigrationStep getStep(String serviceKey) {
        if (interfaceRules != null) {
            InterfaceMigrationRule rule = interfaceRules.get(serviceKey);
            if (rule != null) {
                return rule.getStep() == null ? step : rule.getStep();
            }
        }

        return step;
    }

    public InterfaceMigrationRule getInterfaceRule(String serviceKey) {
        if (interfaceRules == null) {
            return null;
        }
        return interfaceRules.get(serviceKey);
    }

    public MigrationStep getStep() {
        return step;
    }

    public Float getThreshold(String serviceKey) {
        if (interfaceRules != null) {
            InterfaceMigrationRule rule = interfaceRules.get(serviceKey);
            if (rule != null) {
                return rule.getThreshold() == null ? threshold : rule.getThreshold();
            }
        }
        return threshold;
    }

    public Float getThreshold() {
        return threshold;
    }

    public void setThreshold(Float threshold) {
        this.threshold = threshold;
    }

    public Integer getProportion() {
        return proportion;
    }

    public Integer getProportion(String serviceKey) {
        if (interfaceRules != null) {
            InterfaceMigrationRule rule = interfaceRules.get(serviceKey);
            if (rule != null) {
                return rule.getProportion() == null ? proportion : rule.getProportion();
            }
        }
        return proportion;
    }

    public void setProportion(Integer proportion) {
        this.proportion = proportion;
    }

    public Integer getDelay() {
        return delay;
    }

    public Integer getDelay(String serviceKey) {
        if (interfaceRules != null) {
            InterfaceMigrationRule rule = interfaceRules.get(serviceKey);
            if (rule != null) {
                return rule.getDelay() == null ? delay : rule.getDelay();
            }
        }
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public void setStep(MigrationStep step) {
        this.step = step;
    }

    public Boolean getForce() {
        return force;
    }

    public Boolean getForce(String serviceKey) {
        if (interfaceRules != null) {
            InterfaceMigrationRule rule = interfaceRules.get(serviceKey);
            if (rule != null) {
                return rule.getForce() == null ? force : rule.getForce();
            }
        }
        return force;
    }

    public void setForce(Boolean force) {
        this.force = force;
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

    public boolean removeInterfaceRule(String serviceKey) {
        if (CollectionUtils.isNotEmpty(this.interfaces)) {
            boolean removed = this.interfaces.removeIf(interfaceMigrationRule -> interfaceMigrationRule.getServiceKey().equals(serviceKey));
            this.interfaceRules.remove(serviceKey);
            return removed;
        }
        return false;
    }

    public boolean addInterfaceRule(String serviceKey, MigrationStep step, Float threshold, Integer proportion) {
        if (getInterfaceRule(serviceKey) != null) {
            return false;
        }

        if (this.interfaces == null) {
            this.interfaces = new ArrayList<>();
        }
        InterfaceMigrationRule interfaceMigrationRule = new InterfaceMigrationRule(serviceKey, step, threshold, proportion);
        this.interfaces.add(interfaceMigrationRule);

        if (interfaceRules == null) {
            this.interfaceRules = new HashMap<>();
        }
        this.interfaceRules.put(serviceKey, interfaceMigrationRule);
        return true;
    }

    public static MigrationRule parse(String rawRule) {
        Yaml yaml = new Yaml(new SafeConstructor());
        Map<String, Object> map = yaml.load(rawRule);
        return parseFromMap(map);
    }

    public static String toYaml(MigrationRule rule) {
        Constructor constructor = new Constructor(MigrationRule.class);
        Yaml yaml = new Yaml(constructor);
        return yaml.dump(rule);
    }
}
