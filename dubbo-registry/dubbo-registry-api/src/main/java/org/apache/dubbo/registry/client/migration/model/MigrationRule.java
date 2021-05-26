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
import java.util.Set;
import java.util.stream.Collectors;

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
    public static final MigrationRule INIT = new MigrationRule();

    private String key;
    private MigrationStep step;
    private Float threshold;
    // FIXME
    private List<String> targetIps;
    private List<InterfaceMigrationRule> interfaces;
    private List<ApplicationMigrationRule> applications;

    private transient Map<String, InterfaceMigrationRule> interfaceRules;
    private transient Map<String, ApplicationMigrationRule> applicationRules;

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

        Object targetIps = map.get("targetIps");
        if (targetIps != null && List.class.isAssignableFrom(targetIps.getClass())) {
            migrationRule.setTargetIps(((List<Object>) targetIps).stream()
                    .map(String::valueOf).collect(Collectors.toList()));
        }

        Object interfaces = map.get("interfaces");
        if (interfaces != null && List.class.isAssignableFrom(interfaces.getClass())) {
            migrationRule.setInterfaces(((List<Map<String, Object>>) interfaces).stream()
                    .map(InterfaceMigrationRule::parseFromMap).collect(Collectors.toList()));
        }

        Object applications = map.get("applications");
        if (applications != null && List.class.isAssignableFrom(applications.getClass())) {
            migrationRule.setApplications(((List<Map<String, Object>>) applications).stream()
                    .map(ApplicationMigrationRule::parseFromMap).collect(Collectors.toList()));
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

    public MigrationStep getStep(String serviceKey, Set<String> apps) {
        if (interfaceRules != null) {
            InterfaceMigrationRule rule = interfaceRules.get(serviceKey);
            if (rule != null) {
                return rule.getStep() == null ? step : rule.getStep();
            }
        }

        if (apps != null) {
            for (String app : apps) {
                if (applicationRules != null) {
                    ApplicationMigrationRule rule = applicationRules.get(app);
                    if (rule != null) {
                        return rule.getStep() == null ? step : rule.getStep();
                    }
                }
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

    public ApplicationMigrationRule getApplicationRule(String app) {
        if (applicationRules == null) {
            return null;
        }
        return applicationRules.get(app);
    }

    public MigrationStep getStep() {
        return step;
    }

    public Float getThreshold(String serviceKey, Set<String> apps) {
        if (interfaceRules != null) {
            InterfaceMigrationRule rule = interfaceRules.get(serviceKey);
            if (rule != null) {
                return rule.getThreshold() == null ? threshold : rule.getThreshold();
            }
        }

        if (apps != null) {
            for (String app : apps) {
                if (applicationRules != null) {
                    ApplicationMigrationRule rule = applicationRules.get(app);
                    if (rule != null) {
                        return rule.getThreshold() == null ? threshold : rule.getThreshold();
                    }
                }
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

    public void setStep(MigrationStep step) {
        this.step = step;
    }

    public List<InterfaceMigrationRule> getInterfaces() {
        return interfaces;
    }

    public List<String> getTargetIps() {
        return targetIps;
    }

    public void setTargetIps(List<String> targetIps) {
        this.targetIps = targetIps;
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

    public List<ApplicationMigrationRule> getApplications() {
        return applications;
    }

    public void setApplications(List<ApplicationMigrationRule> applications) {
        this.applications = applications;
        if (applications != null) {
            this.applicationRules = new HashMap<>();
            applications.forEach(rule -> {
                applicationRules.put(rule.getName(), rule);
            });
        }
    }

    public boolean removeApplicationRule(String providerApp) {
        if (CollectionUtils.isNotEmpty(this.applications)) {
            boolean removed = this.applications.removeIf(applicationMigrationRule -> applicationMigrationRule.getName().equals(providerApp));
            this.applicationRules.remove(providerApp);
            return removed;
        }
        return false;
    }

    public boolean removeInterfaceRule(String serviceKey) {
        if (CollectionUtils.isNotEmpty(this.interfaces)) {
            boolean removed = this.interfaces.removeIf(interfaceMigrationRule -> interfaceMigrationRule.getServiceKey().equals(serviceKey));
            this.interfaceRules.remove(serviceKey);
            return removed;
        }
        return false;
    }

    public boolean addInterfaceRule(String providerApp, String serviceKey, MigrationStep step, Float threshold) {
        if (getInterfaceRule(serviceKey) != null) {
            return false;
        }

        if (this.interfaces == null) {
            this.interfaces = new ArrayList<>();
        }
        InterfaceMigrationRule interfaceMigrationRule = new InterfaceMigrationRule(providerApp, serviceKey, step, threshold);
        this.interfaces.add(interfaceMigrationRule);

        if (interfaceRules == null) {
            this.interfaceRules = new HashMap<>();
        }
        this.interfaceRules.put(serviceKey, interfaceMigrationRule);
        return true;
    }

    public boolean addApplicationRule(String providerApp, MigrationStep step, Float threshold) {
        if (getApplicationRule(providerApp) != null) {
            return false;
        }

        if (this.applications == null) {
            this.applications = new ArrayList<>();
        }
        ApplicationMigrationRule applicationMigrationRule = new ApplicationMigrationRule(providerApp, step, threshold);
        this.applications.add(applicationMigrationRule);

        if (applicationRules == null) {
            this.applicationRules = new HashMap<>();
        }
        this.applicationRules.put(providerApp, applicationMigrationRule);
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
