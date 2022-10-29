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
package org.apache.dubbo.common.config.configcenter;

import java.util.EventObject;
import java.util.Objects;

/**
 * An event raised when the config changed, immutable.
 *
 * @see ConfigChangeType
 */
public class ConfigChangedEvent extends EventObject {

    private final String key;

    private final String group;

    private final String content;

    private final ConfigChangeType changeType;

    public ConfigChangedEvent(String key, String group, String content) {
        this(key, group, content, ConfigChangeType.MODIFIED);
    }

    public ConfigChangedEvent(String key, String group, String content, ConfigChangeType changeType) {
        super(key + "," + group);
        this.key = key;
        this.group = group;
        this.content = content;
        this.changeType = changeType;
    }

    public String getKey() {
        return key;
    }

    public String getGroup() {
        return group;
    }

    public String getContent() {
        return content;
    }

    public ConfigChangeType getChangeType() {
        return changeType;
    }

    @Override
    public String toString() {
        return "ConfigChangedEvent{" +
                "key='" + key + '\'' +
                ", group='" + group + '\'' +
                ", content='" + content + '\'' +
                ", changeType=" + changeType +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfigChangedEvent)) {
            return false;
        }
        ConfigChangedEvent that = (ConfigChangedEvent) o;
        return Objects.equals(getKey(), that.getKey()) &&
                Objects.equals(getGroup(), that.getGroup()) &&
                Objects.equals(getContent(), that.getContent()) &&
                getChangeType() == that.getChangeType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getGroup(), getContent(), getChangeType());
    }
}
