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

import org.apache.dubbo.common.event.DubboEvent;
import org.apache.dubbo.common.utils.TimePair;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class ConfigCenterChangeEvent extends DubboEvent {

    private final String key;

    private final String group;

    private final String protocol;

    private final ConfigChangeType changeType;

    private final int count;

    private final TimePair timePair;

    public ConfigCenterChangeEvent(
            ApplicationModel source, String key, String group, String protocol, ConfigChangeType changeType) {
        this(source, key, group, protocol, changeType, 1);
    }

    public ConfigCenterChangeEvent(
            ApplicationModel source,
            String key,
            String group,
            String protocol,
            ConfigChangeType changeType,
            int count) {
        super(source);
        this.key = key;
        this.group = group;
        this.protocol = protocol;
        this.changeType = changeType;
        this.count = count;
        this.timePair = TimePair.start();
    }

    public TimePair getTimePair() {
        return timePair;
    }

    public String getKey() {
        return key;
    }

    public String getGroup() {
        return group;
    }

    public String getProtocol() {
        return protocol;
    }

    public ConfigChangeType getChangeType() {
        return changeType;
    }

    public int getCount() {
        return count;
    }
}
