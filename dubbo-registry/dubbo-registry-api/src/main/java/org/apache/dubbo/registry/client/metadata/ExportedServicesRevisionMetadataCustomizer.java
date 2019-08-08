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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.compiler.support.ClassUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceMetadataCustomizer;

import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;

import static java.lang.String.valueOf;
import static java.util.Objects.hash;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_KEY;

/**
 * The customizer to a add the metadata that the reversion of Dubbo exported services calculates.
 * <p>
 * The reversion is calculated on the methods that all Dubbo exported interfaces declare
 *
 * @since 2.7.4
 */
public class ExportedServicesRevisionMetadataCustomizer extends ServiceInstanceMetadataCustomizer {

    @Override
    protected String buildMetadataKey(ServiceInstance serviceInstance) {
        return EXPORTED_SERVICES_REVISION_KEY;
    }

    @Override
    protected String buildMetadataValue(ServiceInstance serviceInstance) {
        WritableMetadataService writableMetadataService = WritableMetadataService.getDefaultExtension();
        SortedSet<String> exportedURLs = writableMetadataService.getExportedURLs();
        Object[] data = exportedURLs.stream()
                .map(URL::valueOf)                       // String to URL
                .map(URL::getServiceInterface)           // get the service interface
                .filter(this::isNotMetadataService)      // filter not MetadataService interface
                .map(ClassUtils::forName)                // load business interface class
                .map(Class::getMethods)                  // get all public methods from business interface
                .map(Arrays::asList)                     // Array to List
                .flatMap(Collection::stream)             // flat Stream<Stream> to be Stream
                .map(Object::toString)                   // Method to String
                .sorted()                                // sort methods marking sure the calculation of reversion is stable
                .toArray();                              // Stream to Array
        return valueOf(hash(data));                      // calculate the hash code as reversion
    }

    private boolean isNotMetadataService(String serviceInterface) {
        return !MetadataService.class.getName().equals(serviceInterface);
    }
}
