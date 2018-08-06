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
package org.apache.dubbo.config.dynamic.parser.model;

import java.util.List;

/**
 *
 */
public class ConfiguratorRule {
    private String service;
    private List<ConfigItem> items;
    // The following attributes don't generated from original configuration
    private String group;
    private String version;
    private String interfaceName;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        if (service == null) {
            throw new IllegalStateException("service field in coniguration is null!");
        }
        this.interfaceName = service;
        int i = service.indexOf("/");
        if (i > 0) {
            this.group = service.substring(0, i);
            service = service.substring(i + 1);
        }
        int j = service.indexOf(":");
        if (j > 0) {
            this.interfaceName = service.substring(0, j);
            this.version = service.substring(j + 1);
        }
    }

    public List<ConfigItem> getItems() {
        return items;
    }

    public void setItems(List<ConfigItem> items) {
        this.items = items;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
}
