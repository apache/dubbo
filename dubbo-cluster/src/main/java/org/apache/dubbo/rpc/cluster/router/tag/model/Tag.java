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
package org.apache.dubbo.rpc.cluster.router.tag.model;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.PojoUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_RULE_PARSING;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_VERSION_V30;

public class Tag {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(Tag.class);

    private String name;
    private List<ParamMatch> match;
    private List<String> addresses;

    @SuppressWarnings("unchecked")
    public static Tag parseFromMap(Map<String, Object> map, String version) {
        Tag tag = new Tag();
        tag.setName((String) map.get("name"));

        if (version != null && version.startsWith(RULE_VERSION_V30)) {
            if (map.get("match") != null) {
                tag.setMatch(((List<Map<String, Object>>) map.get("match")).stream().map((objectMap) -> {
                    try {
                        return PojoUtils.mapToPojo(objectMap, ParamMatch.class);
                    } catch (ReflectiveOperationException e) {
                        logger.error(CLUSTER_FAILED_RULE_PARSING, " Failed to parse tag rule ", String.valueOf(objectMap), "Error occurred when parsing rule component.", e);
                    }
                    return null;
                }).collect(Collectors.toList()));
            } else {
                logger.warn(CLUSTER_FAILED_RULE_PARSING, "", String.valueOf(map), "It's recommended to use 'match' instead of 'addresses' for v3.0 tag rule.");
            }
        }

        Object addresses = map.get("addresses");
        if (addresses != null && List.class.isAssignableFrom(addresses.getClass())) {
            tag.setAddresses(((List<Object>) addresses).stream().map(String::valueOf).collect(Collectors.toList()));
        }

        return tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    public List<ParamMatch> getMatch() {
        return match;
    }

    public void setMatch(List<ParamMatch> match) {
        this.match = match;
    }

}
