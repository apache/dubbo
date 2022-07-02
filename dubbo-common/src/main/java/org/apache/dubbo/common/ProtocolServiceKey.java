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

public class ProtocolServiceKey extends ServiceKey {
    private final String protocol;

    public ProtocolServiceKey(String interfaceName, String group, String version, String protocol) {
        super(interfaceName, group, version);
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getServiceKeyString() {
        return super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ProtocolServiceKey that = (ProtocolServiceKey) o;
        return Objects.equals(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), protocol);
    }

    @Override
    public String toString() {
        return super.toString() + CommonConstants.GROUP_CHAR_SEPARATOR + protocol;
    }

    public static class Matcher {
        public static boolean isMatch(ProtocolServiceKey rule, ProtocolServiceKey target) {
            // 1. match interface (accurate match)
            if (!Objects.equals(rule.getInterfaceName(), target.getInterfaceName())) {
                return false;
            }

            // 2. match version (accurate match)
            if (!Objects.equals(rule.getVersion(), target.getVersion())) {
                return false;
            }

            // match group (wildcard match)
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

            // 4.match protocol
            // 4.1. if rule group is *, match all
            if (!CommonConstants.ANY_VALUE.equals(rule.getProtocol())) {
                // 4.2. if rule protocol is null, match all
                if (rule.getProtocol() != null) {
                    // 4.3. if rule protocol contains ',', split and match each
                    if (rule.getProtocol().contains(CommonConstants.COMMA_SEPARATOR)) {
                        String[] protocols = rule.getProtocol().split("\\" +CommonConstants.COMMA_SEPARATOR, -1);
                        boolean match = false;
                        for (String protocol : protocols) {
                            if (StringUtils.isEmpty(protocol) && StringUtils.isEmpty(target.getProtocol())) {
                                match = true;
                                break;
                            } else if (protocol.equals(target.getProtocol())) {
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            return false;
                        }
                    }
                    // 4.3. if rule protocol is not contains ',', match directly
                    else if (!Objects.equals(rule.getProtocol(), target.getProtocol())) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
