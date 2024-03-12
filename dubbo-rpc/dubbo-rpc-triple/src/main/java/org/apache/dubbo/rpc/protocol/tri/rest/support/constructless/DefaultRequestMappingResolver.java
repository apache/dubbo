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
package org.apache.dubbo.rpc.protocol.tri.rest.support.constructless;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.ServiceVersionCondition;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.DefaultRestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;

@Activate
public class DefaultRequestMappingResolver implements RequestMappingResolver {
    private final RestToolKit toolKit;

    public DefaultRequestMappingResolver(FrameworkModel frameworkModel) {
        this.toolKit = new DefaultRestToolKit(frameworkModel);
    }

    @Override
    public RestToolKit getRestToolKit() {
        return toolKit;
    }

    @Override
    public RequestMapping resolve(ServiceMeta serviceMeta) {
        return RequestMapping.builder()
                .path(serviceMeta.getServiceInterface())
                .name(serviceMeta.getType().getSimpleName())
                .contextPath(serviceMeta.getContextPath())
                .build();
    }

    @Override
    public RequestMapping resolve(MethodMeta methodMeta) {
        ServiceMeta serviceMeta = methodMeta.getServiceMeta();
        return RequestMapping.builder()
                .path(methodMeta.getMethod().getName())
                .name(methodMeta.getMethod().getName())
                .contextPath(methodMeta.getServiceMeta().getContextPath())
                .custom(new ServiceVersionCondition(serviceMeta.getServiceGroup(), serviceMeta.getServiceVersion()))
                .build();
    }
}
