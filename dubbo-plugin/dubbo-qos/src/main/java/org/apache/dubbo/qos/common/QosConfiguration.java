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
package org.apache.dubbo.qos.common;

import org.apache.dubbo.qos.permission.PermissionLevel;

public class QosConfiguration {
    private String welcome;
    private boolean acceptForeignIp;

    // the whitelist of foreign IP when acceptForeignIp = false, the delimiter is colon(,)
    // support specific ip and an ip range from CIDR specification
    private String acceptForeignIpWhitelist;

    // this permission level for anonymous access, it will ignore the acceptForeignIp and acceptForeignIpWhitelist configurations
    // Access permission depends on the config anonymousAccessPermissionLevel and the cmd required permission level
    // the default value is Cmd.PermissionLevel.NONE, can not access any cmd
    private PermissionLevel anonymousAccessPermissionLevel = PermissionLevel.NONE;

    private QosConfiguration() {
    }

    public QosConfiguration(Builder builder) {
        this.welcome = builder.getWelcome();
        this.acceptForeignIp = builder.isAcceptForeignIp();
        this.acceptForeignIpWhitelist = builder.getAcceptForeignIpWhitelist();
        this.anonymousAccessPermissionLevel = builder.getAnonymousAccessPermissionLevel();
    }

    public boolean isAllowAnonymousAccess() {
        return PermissionLevel.NONE != anonymousAccessPermissionLevel;
    }

    public String getWelcome() {
        return welcome;
    }

    public PermissionLevel getAnonymousAccessPermissionLevel() {
        return anonymousAccessPermissionLevel;
    }

    public String getAcceptForeignIpWhitelist() {
        return acceptForeignIpWhitelist;
    }

    public boolean isAcceptForeignIp() {
        return acceptForeignIp;
    }

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {
        private String welcome;
        private boolean acceptForeignIp;
        private String acceptForeignIpWhitelist;
        private PermissionLevel anonymousAccessPermissionLevel = PermissionLevel.NONE;

        private Builder() {
        }

        public Builder welcome(String welcome) {
            this.welcome = welcome;
            return this;
        }

        public Builder acceptForeignIp(boolean acceptForeignIp) {
            this.acceptForeignIp = acceptForeignIp;
            return this;
        }

        public Builder acceptForeignIpWhitelist(String acceptForeignIpWhitelist) {
            this.acceptForeignIpWhitelist = acceptForeignIpWhitelist;
            return this;
        }

        public Builder anonymousAccessPermissionLevel(String anonymousAccessPermissionLevel) {
            this.anonymousAccessPermissionLevel = PermissionLevel.from(anonymousAccessPermissionLevel);
            return this;
        }

        public QosConfiguration build() {
            return new QosConfiguration(this);
        }

        public String getWelcome() {
            return welcome;
        }

        public boolean isAcceptForeignIp() {
            return acceptForeignIp;
        }

        public String getAcceptForeignIpWhitelist() {
            return acceptForeignIpWhitelist;
        }

        public PermissionLevel getAnonymousAccessPermissionLevel() {
            return anonymousAccessPermissionLevel;
        }
    }
}
