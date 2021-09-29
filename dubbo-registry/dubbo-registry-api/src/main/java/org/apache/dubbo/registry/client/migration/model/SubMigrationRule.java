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

import java.util.Map;

import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_DELAY_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_FORCE_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_PROPORTION_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_STEP_KEY;
import static org.apache.dubbo.registry.Constants.MIGRATION_RULE_THRESHOLD_KEY;

public class SubMigrationRule {
    private String serviceKey;
    private MigrationStep step;
    private Float threshold;
    private Integer proportion;
    private Integer delay;
    private Boolean force;

    public static SubMigrationRule parseFromMap(Map<String, Object> map) {
        SubMigrationRule interfaceMigrationRule = new SubMigrationRule();
        interfaceMigrationRule.setServiceKey((String) map.get("serviceKey"));

        Object step = map.get(MIGRATION_RULE_STEP_KEY);
        if (step != null) {
            interfaceMigrationRule.setStep(MigrationStep.valueOf(step.toString()));
        }

        Object threshold = map.get(MIGRATION_RULE_THRESHOLD_KEY);
        if (threshold != null) {
            interfaceMigrationRule.setThreshold(Float.valueOf(threshold.toString()));
        }

        Object proportion = map.get(MIGRATION_RULE_PROPORTION_KEY);
        if (proportion != null) {
            interfaceMigrationRule.setProportion(Integer.valueOf(proportion.toString()));
        }

        Object delay = map.get(MIGRATION_RULE_DELAY_KEY);
        if (delay != null) {
            interfaceMigrationRule.setDelay(Integer.valueOf(delay.toString()));
        }

        Object force = map.get(MIGRATION_RULE_FORCE_KEY);
        if (force != null) {
            interfaceMigrationRule.setForce(Boolean.valueOf(force.toString()));
        }

        return interfaceMigrationRule;
    }

    public SubMigrationRule(){}

    public SubMigrationRule(String serviceKey, MigrationStep step, Float threshold, Integer proportion) {
        this.serviceKey = serviceKey;
        this.step = step;
        this.threshold = threshold;
        this.proportion = proportion;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public MigrationStep getStep() {
        return step;
    }

    public void setStep(MigrationStep step) {
        this.step = step;
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

    public void setProportion(Integer proportion) {
        this.proportion = proportion;
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public Boolean getForce() {
        return force;
    }

    public void setForce(Boolean force) {
        this.force = force;
    }
}
