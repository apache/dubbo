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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.ServiceNameMapping;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.dubbo.registry.Constants.MIGRATION_DELAY_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_FORCE_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_PROMOTION_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_APPLICATIONS_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_DELAY_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_FORCE_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_INTERFACES_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_PROPORTION_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_STEP_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_THRESHOLD_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_STEP_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_THRESHOLD_KEY;
import static org.apache.dubbo.registry.client.migration.MigrationRuleHandler.DUBBO_SERVICEDISCOVERY_MIGRATION;

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
 * applications:
 *  - serviceKey: TestApplication
 *    threshold: 0.3
 *    proportion: 20
 *    delay: 10
 *    force: false
 *    step: FORCE_INTERFACE
 */
public class MigrationRule {

    private String key;
    private MigrationStep step;
    private Float threshold;
    private Integer proportion;
    private Integer delay;
    private Boolean force;
    private List<SubMigrationRule> interfaces;
    private List<SubMigrationRule> applications;

    private transient Map<String, SubMigrationRule> interfaceRules;
    private transient Map<String, SubMigrationRule> applicationRules;

    public static MigrationRule getInitRule() {
        return new MigrationRule();
    }

    @SuppressWarnings("unchecked")
    private static MigrationRule parseFromMap(Map<String, Object> map) {
        MigrationRule migrationRule = new MigrationRule();
        migrationRule.setKey((String) map.get(MIGRATION_RULE_KEY));

        Object step = map.get(MIGRATION_RULE_STEP_KEY);
        if (step != null) {
            migrationRule.setStep(MigrationStep.valueOf(step.toString()));
        }

        Object threshold = map.get(MIGRATION_RULE_THRESHOLD_KEY);
        if (threshold != null) {
            migrationRule.setThreshold(Float.valueOf(threshold.toString()));
        }

        Object proportion = map.get(MIGRATION_RULE_PROPORTION_KEY);
        if (proportion != null) {
            migrationRule.setProportion(Integer.valueOf(proportion.toString()));
        }

        Object delay = map.get(MIGRATION_RULE_DELAY_KEY);
        if (delay != null) {
            migrationRule.setDelay(Integer.valueOf(delay.toString()));
        }

        Object force = map.get(MIGRATION_RULE_FORCE_KEY);
        if (force != null) {
            migrationRule.setForce(Boolean.valueOf(force.toString()));
        }

        Object interfaces = map.get(MIGRATION_RULE_INTERFACES_KEY);
        if (interfaces != null && List.class.isAssignableFrom(interfaces.getClass())) {
            migrationRule.setInterfaces(((List<Map<String, Object>>) interfaces).stream()
                    .map(SubMigrationRule::parseFromMap).collect(Collectors.toList()));
        }

        Object applications = map.get(MIGRATION_RULE_APPLICATIONS_KEY);
        if (applications != null && List.class.isAssignableFrom(applications.getClass())) {
            migrationRule.setApplications(((List<Map<String, Object>>) applications).stream()
                .map(SubMigrationRule::parseFromMap).collect(Collectors.toList()));
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

    public MigrationStep getStep(URL consumerURL) {
        MigrationStep value = getValue(consumerURL, SubMigrationRule::getStep);
        if (value != null) {
            return value;
        }

        /**
         * FIXME, it's really hard to follow setting default values here.
         */
        if (step == null) {
            // initial step : APPLICATION_FIRST
            step = MigrationStep.APPLICATION_FIRST;
            step = Enum.valueOf(MigrationStep.class,
                consumerURL.getParameter(MIGRATION_STEP_KEY, getDefaultStep(consumerURL, step.name())));
        }

        return step;
    }

    private String getDefaultStep(URL consumerURL, String defaultStep) {
        String globalDefaultStep = ConfigurationUtils.getCachedDynamicProperty(consumerURL.getScopeModel(), DUBBO_SERVICEDISCOVERY_MIGRATION, null);
        if (StringUtils.isEmpty(globalDefaultStep)) {
            // check 'dubbo.application.service-discovery.migration' for compatibility
            globalDefaultStep = ConfigurationUtils.getCachedDynamicProperty(consumerURL.getScopeModel(), "dubbo.application.service-discovery.migration", defaultStep);
        }
        return globalDefaultStep;
    }

    public MigrationStep getStep() {
        return step;
    }

    public float getThreshold(URL consumerURL) {
        Float value = getValue(consumerURL, SubMigrationRule::getThreshold);
        if (value != null) {
            return value;
        }

        return threshold == null ? consumerURL.getParameter(MIGRATION_THRESHOLD_KEY, -1f) : threshold;
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

    public int getProportion(URL consumerURL) {
        Integer value = getValue(consumerURL, SubMigrationRule::getProportion);
        if (value != null) {
            return value;
        }

        return proportion == null ? consumerURL.getParameter(MIGRATION_PROMOTION_KEY, 100) : proportion;
    }

    public void setProportion(Integer proportion) {
        this.proportion = proportion;
    }

    public Integer getDelay() {
        return delay;
    }

    public int getDelay(URL consumerURL) {
        Integer value = getValue(consumerURL, SubMigrationRule::getDelay);
        if (value != null) {
            return value;
        }

        return delay == null ? consumerURL.getParameter(MIGRATION_DELAY_KEY, 0) : delay;
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

    public boolean getForce(URL consumerURL) {
        Boolean value = getValue(consumerURL, SubMigrationRule::getForce);
        if (value != null) {
            return value;
        }

        return force == null ? consumerURL.getParameter(MIGRATION_FORCE_KEY, false) : force;
    }

    public <T> T getValue(URL consumerURL, Function<SubMigrationRule, T> function) {
        if (interfaceRules != null) {
            SubMigrationRule rule = interfaceRules.get(consumerURL.getDisplayServiceKey());
            if (rule != null) {
                T value = function.apply(rule);
                if (value != null) {
                    return value;
                }
            }
        }

        if (applications != null) {
            ServiceNameMapping serviceNameMapping = ServiceNameMapping.getDefaultExtension(consumerURL.getScopeModel());
            Set<String> services = serviceNameMapping.getRemoteMapping(consumerURL);
            if (CollectionUtils.isNotEmpty(services)) {
                for (String service : services) {
                    SubMigrationRule rule = applicationRules.get(service);
                    if (rule != null) {
                        T value = function.apply(rule);
                        if (value != null) {
                            return value;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void setForce(Boolean force) {
        this.force = force;
    }

    public List<SubMigrationRule> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<SubMigrationRule> interfaces) {
        this.interfaces = interfaces;
        if (interfaces != null) {
            this.interfaceRules = new HashMap<>();
            interfaces.forEach(rule -> {
                interfaceRules.put(rule.getServiceKey(), rule);
            });
        }
    }

    public List<SubMigrationRule> getApplications() {
        return applications;
    }

    public void setApplications(List<SubMigrationRule> applications) {
        this.applications = applications;
        if (applications != null) {
            this.applicationRules = new HashMap<>();
            applications.forEach(rule -> {
                applicationRules.put(rule.getServiceKey(), rule);
            });
        }
    }

    public static MigrationRule parse(String rawRule) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> map = yaml.load(rawRule);
        return parseFromMap(map);
    }

    public static String toYaml(MigrationRule rule) {
        Constructor constructor = new Constructor(MigrationRule.class, new LoaderOptions());
        Yaml yaml = new Yaml(constructor);
        return yaml.dump(rule);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MigrationRule that = (MigrationRule) o;
        return Objects.equals(key, that.key) && step == that.step && Objects.equals(threshold, that.threshold) && Objects.equals(proportion, that.proportion) && Objects.equals(delay, that.delay) && Objects.equals(force, that.force) && Objects.equals(interfaces, that.interfaces) && Objects.equals(applications, that.applications) && Objects.equals(interfaceRules, that.interfaceRules) && Objects.equals(applicationRules, that.applicationRules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, step, threshold, proportion, delay, force, interfaces, applications, interfaceRules, applicationRules);
    }
}
