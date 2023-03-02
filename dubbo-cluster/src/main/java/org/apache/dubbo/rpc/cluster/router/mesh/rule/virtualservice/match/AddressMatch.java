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
package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.net.UnknownHostException;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_EXEC_CONDITION_ROUTER;
import static org.apache.dubbo.common.utils.NetUtils.matchIpExpression;
import static org.apache.dubbo.common.utils.UrlUtils.isMatchGlobPattern;

public class AddressMatch {
    public static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AddressMatch.class);
    private String wildcard;
    private String cird;
    private String exact;

    public String getWildcard() {
        return wildcard;
    }

    public void setWildcard(String wildcard) {
        this.wildcard = wildcard;
    }

    public String getCird() {
        return cird;
    }

    public void setCird(String cird) {
        this.cird = cird;
    }

    public String getExact() {
        return exact;
    }

    public void setExact(String exact) {
        this.exact = exact;
    }

    public boolean isMatch(String input) {
        if (getCird() != null && input != null) {
            try {
                return input.equals(getCird()) || matchIpExpression(getCird(), input);
            } catch (UnknownHostException e) {
                logger.error(CLUSTER_FAILED_EXEC_CONDITION_ROUTER, "Executing routing rule match expression error.", "", String.format("Error trying to match cird formatted address %s with input %s in AddressMatch.", getCird(), input), e);
            }
        }
        if (getWildcard() != null && input != null) {
            if (ANYHOST_VALUE.equals(getWildcard()) || ANY_VALUE.equals(getWildcard())) {
                return true;
            }
            // FIXME
            return isMatchGlobPattern(getWildcard(), input);
        }
        if (getExact() != null && input != null) {
            return input.equals(getExact());
        }
        return false;
    }
}
