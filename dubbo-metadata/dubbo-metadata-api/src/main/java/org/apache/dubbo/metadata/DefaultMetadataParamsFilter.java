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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.extension.Activate;

import static org.apache.dubbo.common.constants.CommonConstants.IPV6_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.FilterConstants.VALIDATION_KEY;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.remoting.Constants.HEARTBEAT_TIMEOUT_KEY;
import static org.apache.dubbo.rpc.Constants.INTERFACES;

@Activate
public class DefaultMetadataParamsFilter implements MetadataParamsFilter {
    private final String[] excludedServiceParams;
    private final String[] includedInstanceParams;

    public DefaultMetadataParamsFilter() {
        this.includedInstanceParams = new String[]{HEARTBEAT_TIMEOUT_KEY, TIMESTAMP_KEY, IPV6_KEY};
        this.excludedServiceParams = new String[]{MONITOR_KEY, BIND_IP_KEY, BIND_PORT_KEY, QOS_ENABLE,
            QOS_HOST, QOS_PORT, ACCEPT_FOREIGN_IP, VALIDATION_KEY, INTERFACES, PID_KEY, TIMESTAMP_KEY, HEARTBEAT_TIMEOUT_KEY,
            IPV6_KEY};
    }

    @Override
    public String[] instanceParamsIncluded() {
        return includedInstanceParams;
    }

    @Override
    public String[] serviceParamsExcluded() {
        return excludedServiceParams;
    }
}
