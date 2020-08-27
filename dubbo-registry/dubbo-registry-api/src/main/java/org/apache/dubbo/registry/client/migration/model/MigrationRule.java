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

/**
 * # key = demo-consumer.migration
 * # group = DUBBO_SERVICEDISCOVERY_MIGRATION
 * # content
 * key: demo-consumer
 * step: APPLICATION_FIRST
 */
public class MigrationRule {
    private String key;
    private MigrationStep step;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public MigrationStep getStep() {
        return step;
    }

    public void setStep(MigrationStep step) {
        this.step = step;
    }

    public static MigrationRule parse(String rawRule) {
        Constructor constructor = new Constructor(MigrationRule.class);
        Yaml yaml = new Yaml(constructor);
        return yaml.load(rawRule);
    }
}
