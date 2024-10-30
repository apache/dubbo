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
package org.apache.dubbo.spring.boot.actuate.endpoint;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "management.endpoint")
public class DubboActuatorProperties {

    private Map<String, Boolean> dubbo;

    public Map<String, Boolean> getDubbo() {
        return dubbo;
    }

    public void setDubbo(Map<String, Boolean> dubbo) {
        this.dubbo = dubbo;
    }

    public boolean isEnabled(String command) {
        if (StringUtils.hasText(command)) {
            Boolean enabled = dubbo.get(command + ".enabled");
            return enabled != null && enabled;
        } else {
            return false;
        }
    }
}
