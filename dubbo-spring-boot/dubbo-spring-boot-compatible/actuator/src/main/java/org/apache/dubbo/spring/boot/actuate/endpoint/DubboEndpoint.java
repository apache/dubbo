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


import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboMetadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Actuator {@link Endpoint} to expose Dubbo Meta Data
 *
 * @see Endpoint
 * @since 2.7.0
 */
@ConfigurationProperties(prefix = "endpoints.dubbo", ignoreUnknownFields = false)
public class DubboEndpoint extends AbstractEndpoint<Map<String, Object>> {

    @Autowired
    private DubboMetadata dubboMetadata;

    public DubboEndpoint() {
        super("dubbo", true, false);
    }

    @Override
    public Map<String, Object> invoke() {
        return dubboMetadata.invoke();
    }
}
