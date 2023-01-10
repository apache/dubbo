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
package org.apache.dubbo.rpc.protocol.rest.annotation.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.rest.PathMatcher;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadataResolver;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.rest.exception.CodeStyleNotSupportException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class MetadataResolver {
    // TODO  choose resolver by url code Style ?
    private static Set<ServiceRestMetadataResolver> serviceRestMetadataResolvers =
        ApplicationModel.defaultModel().getExtensionLoader(ServiceRestMetadataResolver.class).getSupportedExtensionInstances();

    /**
     * for consumer
     *
     * @param serviceImpl
     * @return
     */
    public static Map<Method, RestMethodMetadata> resolveConsumerServiceMetadata(Class serviceImpl, URL url) {

        for (ServiceRestMetadataResolver serviceRestMetadataResolver : serviceRestMetadataResolvers) {
            boolean supports = serviceRestMetadataResolver.supports(serviceImpl, true);
            if (supports) {
                ServiceRestMetadata serviceRestMetadata = new ServiceRestMetadata(url.getServiceInterface(), url.getVersion(), url.getGroup(), true);
                ServiceRestMetadata resolve = serviceRestMetadataResolver.resolve(serviceImpl, serviceRestMetadata);
                return resolve.getMethodToServiceMap();
            }
        }
        throw new CodeStyleNotSupportException("service is:" + serviceImpl + ",just support rest or spring-web annotation");
    }


}
