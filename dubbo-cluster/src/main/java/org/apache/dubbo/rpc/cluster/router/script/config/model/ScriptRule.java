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
package org.apache.dubbo.rpc.cluster.router.script.config.model;

import org.apache.dubbo.rpc.cluster.router.AbstractRouterRule;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.Map;

public class ScriptRule extends AbstractRouterRule {
    private static final String TYPE_KEY = "type";
    private static final String SCRIPT_KEY = "script";
    private String type;
    private String script;

    public static ScriptRule parse(String rawRule) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> map = yaml.load(rawRule);

        ScriptRule rule = new ScriptRule();
        rule.parseFromMap0(map);
        rule.setRawRule(rawRule);

        Object rawType = map.get(TYPE_KEY);
        if (rawType != null) {
            rule.setType((String) rawType);
        }

        Object rawScript = map.get(SCRIPT_KEY);
        if (rawScript != null) {
            rule.setScript((String) rawScript);
        } else {
            rule.setValid(false);
        }

        return rule;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}
