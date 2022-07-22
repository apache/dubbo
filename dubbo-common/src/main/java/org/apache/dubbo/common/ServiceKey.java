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
package org.apache.dubbo.common;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Objects;

public class ServiceKey {
    private final String interfaceName;
    private final String group;
    private final String version;

    public ServiceKey(String interfaceName, String version, String group) {
        this.interfaceName = interfaceName;
        this.group = group;
        this.version = version;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceKey that = (ServiceKey) o;
        return Objects.equals(interfaceName, that.interfaceName) && Objects.equals(group, that.group) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interfaceName, group, version);
    }

    @Override
    public String toString() {
        return BaseServiceMetadata.buildServiceKey(interfaceName, group, version);
    }


    public static class Matcher {
        public static boolean isMatch(ServiceKey rule, ServiceKey target) {
            // 1. match interface (accurate match)
            if (!Objects.equals(rule.getInterfaceName(), target.getInterfaceName())) {
                return false;
            }

            // 2. match version (accurate match)
            // 2.1. if rule version is *, match all
            if (!CommonConstants.ANY_VALUE.equals(rule.getVersion())) {
                // 2.2. if rule version is null, target version should be null
                if (StringUtils.isEmpty(rule.getVersion()) && !StringUtils.isEmpty(target.getVersion())) {
                    return false;
                }
                if (!StringUtils.isEmpty(rule.getVersion())) {
                    // 2.3. if rule version contains ',', split and match each
                    if (rule.getVersion().contains(CommonConstants.COMMA_SEPARATOR)) {
                        String[] versions = rule.getVersion().split("\\" +CommonConstants.COMMA_SEPARATOR, -1);
                        boolean match = false;
                        for (String version : versions) {
                            version = version.trim();
                            if (StringUtils.isEmpty(version) && StringUtils.isEmpty(target.getVersion())) {
                                match = true;
                                break;
                            } else if (version.equals(target.getVersion())) {
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            return false;
                        }
                    }
                    // 2.4. if rule version is not contains ',', match directly
                    else if (!Objects.equals(rule.getVersion(), target.getVersion())) {
                        return false;
                    }
                }
            }

            // 3. match group (wildcard match)
            // 3.1. if rule group is *, match all
            if (!CommonConstants.ANY_VALUE.equals(rule.getGroup())) {
                // 3.2. if rule group is null, target group should be null
                if (StringUtils.isEmpty(rule.getGroup()) && !StringUtils.isEmpty(target.getGroup())) {
                    return false;
                }
                if (!StringUtils.isEmpty(rule.getGroup())) {
                    // 3.3. if rule group contains ',', split and match each
                    if (rule.getGroup().contains(CommonConstants.COMMA_SEPARATOR)) {
                        String[] groups = rule.getGroup().split("\\" +CommonConstants.COMMA_SEPARATOR, -1);
                        boolean match = false;
                        for (String group : groups) {
                            group = group.trim();
                            if (StringUtils.isEmpty(group) && StringUtils.isEmpty(target.getGroup())) {
                                match = true;
                                break;
                            } else if (group.equals(target.getGroup())) {
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            return false;
                        }
                    }
                    // 3.4. if rule group is not contains ',', match directly
                    else if (!Objects.equals(rule.getGroup(), target.getGroup())) {
                        return false;
                    }
                }
            }

            return true;
        }
    }
}
