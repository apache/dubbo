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
package org.apache.dubbo.config.dynamic;

/**
 *
 */
public class ConfigChangeEvent {
    private String key;
    private String newValue;
    private ConfigChangeType changeType;
    private ConfigType type;

    public ConfigChangeEvent(String key, String value, ConfigType type) {
        this(key, value, type, ConfigChangeType.MODIFIED);
    }

    public ConfigChangeEvent(String key, String value, ConfigType type, ConfigChangeType changeType) {
        this.key = key;
        this.newValue = value;
        this.type = type;
        this.changeType = changeType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public ConfigChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ConfigChangeType changeType) {
        this.changeType = changeType;
    }

    public ConfigType getType() {
        return type;
    }

    public void setType(ConfigType type) {
        this.type = type;
    }
}
