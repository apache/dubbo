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
package org.apache.dubbo.xds.util;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.xds.resource.route.HashPolicy;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class HashUtils {

    private HashUtils() {
    }

    public static long generateHash(List<HashPolicy> hashPolicies, Invocation invocation) {
        if (CollectionUtils.isEmpty(hashPolicies)) {
            return ThreadLocalRandom.current().nextLong();
        }

        Long hash = null;
        for (HashPolicy policy : hashPolicies) {
            Long newHash = null;
            
            switch (policy.getType()) {
                case HEADER:
                    String headerValue = getHeaderValue(invocation, policy.getHeaderName());
                    if (headerValue != null) {
                        if (policy.getRegEx() != null && policy.getRegExSubstitution() != null) {
                            headerValue = policy.getRegEx().matcher(headerValue).replaceAll(policy.getRegExSubstitution());
                        }
                        newHash = (long) headerValue.hashCode();
                    }
                    break;
                case COOKIE:
                    String cookieValue = invocation.getAttachment(policy.getHeaderName());
                    if (cookieValue != null) {
                        newHash = (long) cookieValue.hashCode();
                    }
                    break;
                case QUERY_PARAMETER:
                    String queryParam = invocation.getInvoker().getUrl().getParameter(policy.getHeaderName());
                    if (queryParam != null) {
                        newHash = (long) queryParam.hashCode();
                    }
                    break;
                case FILTER_STATE:
                    String filterState = (String) invocation.getAttachment(policy.getHeaderName());
                    if (filterState != null) {
                        newHash = (long) filterState.hashCode();
                    }
                    break;
                case CONNECTION_PROPERTIES:
                    break;
                default:
                    break;
            }

            if (newHash != null) {
                long oldHash = (hash != null) ? ((hash << 1L) | (hash >>> 63L)) : 0;
                hash = oldHash ^ newHash;
            }
            
            if (policy.isTerminal() && hash != null) {
                break;
            }
        }
        
        return hash != null ? hash : ThreadLocalRandom.current().nextLong();
    }

    private static String getHeaderValue(Invocation invocation, String headerName) {
        Object attachment = invocation.getAttachment(headerName);
        if (attachment instanceof String) {
            return (String) attachment;
        }
        
        URL invokerUrl = invocation.getInvoker().getUrl();
        return invokerUrl.getParameter(headerName);
    }
}