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

import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.AbstractDubboMetadata;
import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboConfigsMetadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.Map;

/**
 * Dubbo Configs Metadata {@link Endpoint}
 *
 * @since 2.7.0
 */
@Endpoint(id = "dubboconfigs")
public class DubboConfigsMetadataEndpoint extends AbstractDubboMetadata {

    @Autowired
    private DubboConfigsMetadata dubboConfigsMetadata;

    @ReadOperation
    public Map<String, Map<String, Map<String, Object>>> configs() {
        return dubboConfigsMetadata.configs();
    }
}
