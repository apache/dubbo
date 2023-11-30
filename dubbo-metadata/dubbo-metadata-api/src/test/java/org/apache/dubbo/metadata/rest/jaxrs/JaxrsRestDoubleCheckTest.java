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
package org.apache.dubbo.metadata.rest.jaxrs;

import org.apache.dubbo.metadata.rest.PathMatcher;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.metadata.rest.api.JaxrsRestDoubleCheckContainsPathVariableService;
import org.apache.dubbo.metadata.rest.api.JaxrsRestDoubleCheckService;
import org.apache.dubbo.metadata.rest.api.JaxrsUsingService;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JaxrsRestDoubleCheckTest {
    private JAXRSServiceRestMetadataResolver instance =
            new JAXRSServiceRestMetadataResolver(ApplicationModel.defaultModel());

    @Test
    void testDoubleCheckException() {

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ServiceRestMetadata resolve = new ServiceRestMetadata();
            resolve.setServiceInterface(JaxrsRestDoubleCheckService.class.getName());
            instance.resolve(JaxrsRestDoubleCheckService.class, resolve);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ServiceRestMetadata resolve = new ServiceRestMetadata();
            resolve.setServiceInterface(JaxrsRestDoubleCheckContainsPathVariableService.class.getName());
            instance.resolve(JaxrsRestDoubleCheckContainsPathVariableService.class, resolve);
        });
    }

    @Test
    void testSameHttpMethodException() {

        Assertions.assertDoesNotThrow(() -> {
            ServiceRestMetadata resolve = new ServiceRestMetadata();
            resolve.setServiceInterface(JaxrsUsingService.class.getName());
            instance.resolve(JaxrsUsingService.class, resolve);
        });

        ServiceRestMetadata resolve = new ServiceRestMetadata();
        resolve.setServiceInterface(JaxrsUsingService.class.getName());
        instance.resolve(JaxrsUsingService.class, resolve);

        Map<PathMatcher, RestMethodMetadata> pathContainPathVariableToServiceMap =
                resolve.getPathContainPathVariableToServiceMap();

        RestMethodMetadata restMethodMetadata = pathContainPathVariableToServiceMap.get(
                PathMatcher.getInvokeCreatePathMatcher("/usingService/aaa", null, null, null, "TEST"));

        Assertions.assertNotNull(restMethodMetadata);
    }
}
