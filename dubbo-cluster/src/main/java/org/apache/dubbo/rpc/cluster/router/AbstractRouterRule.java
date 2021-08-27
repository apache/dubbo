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
package org.apache.dubbo.rpc.cluster.router;

import java.util.Map;

import static org.apache.dubbo.rpc.cluster.Constants.DYNAMIC_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.ENABLED_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.FORCE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.KEY_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.PRIORITY_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RAW_RULE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RUNTIME_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.SCOPE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.VALID_KEY;

/**
 * TODO Extract more code here if necessary
 */
public abstract class AbstractRouterRule {
    private String rawRule;
    private boolean runtime = true;
    private boolean force = false;
    private boolean valid = true;
    private boolean enabled = true;
    private int priority;
    private boolean dynamic = false;

    private String scope;
    private String key;

    protected void parseFromMap0(Map<String, Object> map) {
        setRawRule((String) map.get(RAW_RULE_KEY));

        Object runtime = map.get(RUNTIME_KEY);
        if (runtime != null) {
            setRuntime(Boolean.parseBoolean(runtime.toString()));
        }

        Object force = map.get(FORCE_KEY);
        if (force != null) {
            setForce(Boolean.parseBoolean(force.toString()));
        }

        Object valid = map.get(VALID_KEY);
        if (valid != null) {
            setValid(Boolean.parseBoolean(valid.toString()));
        }

        Object enabled = map.get(ENABLED_KEY);
        if (enabled != null) {
            setEnabled(Boolean.parseBoolean(enabled.toString()));
        }

        Object priority = map.get(PRIORITY_KEY);
        if (priority != null) {
            setPriority(Integer.parseInt(priority.toString()));
        }

        Object dynamic = map.get(DYNAMIC_KEY);
        if (dynamic != null) {
            setDynamic(Boolean.parseBoolean(dynamic.toString()));
        }

        setScope((String) map.get(SCOPE_KEY));
        setKey((String) map.get(KEY_KEY));
    }

    public String getRawRule() {
        return rawRule;
    }

    public void setRawRule(String rawRule) {
        this.rawRule = rawRule;
    }

    public boolean isRuntime() {
        return runtime;
    }

    public void setRuntime(boolean runtime) {
        this.runtime = runtime;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
