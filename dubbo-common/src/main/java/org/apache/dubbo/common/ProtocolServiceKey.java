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

    public ProtocolServiceKey(String interfaceName, String version, String group, String protocol) {
        super(interfaceName, version, group);
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getServiceKeyString() {
        return super.toString();
    }

    public boolean isSameWith(ProtocolServiceKey protocolServiceKey) {
        // interface version group should be the same
        if (!super.equals(protocolServiceKey)) {
            return false;
        }

        // origin protocol is *, can not match any protocol
        if (CommonConstants.ANY_VALUE.equals(protocol)) {
            return false;
        }

        // origin protocol is null, can match any protocol
        if (StringUtils.isEmpty(protocol) || StringUtils.isEmpty(protocolServiceKey.getProtocol())) {
            return true;
        }

        // origin protocol is not *, match itself
        return Objects.equals(protocol, protocolServiceKey.getProtocol());
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
            // 1. 2. 3. match interface / version / group
            if (!ServiceKey.Matcher.isMatch(rule, target)) {
                return false;
            }

            // 4.match protocol
            // 4.1. if rule group is *, match all
            if (!CommonConstants.ANY_VALUE.equals(rule.getProtocol())) {
                // 4.2. if rule protocol is null, match all
                if (StringUtils.isNotEmpty(rule.getProtocol())) {
                    // 4.3. if rule protocol contains ',', split and match each
                    if (rule.getProtocol().contains(CommonConstants.COMMA_SEPARATOR)) {
                        String[] protocols = rule.getProtocol().split("\\" +CommonConstants.COMMA_SEPARATOR, -1);
                        boolean match = false;
                        for (String protocol : protocols) {
                            protocol = protocol.trim();
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
