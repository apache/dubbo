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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.metadata.ParameterTypesComparator;
import org.apache.dubbo.metadata.rest.PathMatcher;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadataResolver;
import org.apache.dubbo.rpc.protocol.rest.exception.CodeStyleNotSupportException;

import java.util.Map;

public class MetadataResolver {
    private MetadataResolver() {
    }

    /**
     * for consumer
     *
     * @param targetClass target service class
     * @param url consumer url
     * @return rest metadata
     * @throws CodeStyleNotSupportException not support type
     */
    public static Map<String, Map<ParameterTypesComparator, RestMethodMetadata>> resolveConsumerServiceMetadata(Class<?> targetClass, URL url) {
        ExtensionLoader<ServiceRestMetadataResolver> extensionLoader = url.getOrDefaultApplicationModel().getExtensionLoader(ServiceRestMetadataResolver.class);

        for (ServiceRestMetadataResolver serviceRestMetadataResolver : extensionLoader.getSupportedExtensionInstances()) {
            if (serviceRestMetadataResolver.supports(targetClass, true)) {
                ServiceRestMetadata serviceRestMetadata = new ServiceRestMetadata(url.getServiceInterface(), url.getVersion(), url.getGroup(), true);
                ServiceRestMetadata resolve = serviceRestMetadataResolver.resolve(targetClass, serviceRestMetadata);
                return resolve.getMethodToServiceMap();
            }
        }

        // TODO support Dubbo style service
        throw new CodeStyleNotSupportException("service is: " + targetClass + ", only support " + extensionLoader.getSupportedExtensions() + " annotation");
    }




    public static Map<PathMatcher, RestMethodMetadata> resolveProviderServiceMetadata(Class serviceImpl, URL url) {
        ExtensionLoader<ServiceRestMetadataResolver> extensionLoader = url.getOrDefaultApplicationModel().getExtensionLoader(ServiceRestMetadataResolver.class);

        for (ServiceRestMetadataResolver serviceRestMetadataResolver : extensionLoader.getSupportedExtensionInstances()) {
            boolean supports = serviceRestMetadataResolver.supports(serviceImpl);
            if (supports) {
                ServiceRestMetadata serviceRestMetadata = new ServiceRestMetadata(url.getServiceInterface(), url.getVersion(), url.getGroup(), false);
                ServiceRestMetadata resolve = serviceRestMetadataResolver.resolve(serviceImpl, serviceRestMetadata);
                return resolve.getPathToServiceMap();
            }
        }
        throw new CodeStyleNotSupportException("service is:" + serviceImpl + ",just support rest or spring-web annotation");
    }

}
