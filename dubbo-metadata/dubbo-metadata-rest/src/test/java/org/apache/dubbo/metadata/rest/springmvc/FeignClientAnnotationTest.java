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
package org.apache.dubbo.metadata.rest.springmvc;

import org.apache.dubbo.metadata.rest.PathMatcher;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.metadata.rest.feign.FeignClientController;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FeignClientAnnotationTest {

    private SpringMvcServiceRestMetadataResolver instance =
            new SpringMvcServiceRestMetadataResolver(ApplicationModel.defaultModel());

    @Test
    void testFeignClientAnnotationResolve() {

        Assertions.assertEquals(true, instance.supports(FeignClientController.class));
        Class service = FeignClientController.class;
        ServiceRestMetadata serviceRestMetadata = new ServiceRestMetadata();
        serviceRestMetadata.setServiceInterface(service.getName());

        ServiceRestMetadata resolve = instance.resolve(service, serviceRestMetadata);

        Map<PathMatcher, RestMethodMetadata> unContainPathVariableToServiceMap =
                resolve.getPathUnContainPathVariableToServiceMap();
        RestMethodMetadata restMethodMetadata = unContainPathVariableToServiceMap.get(
                PathMatcher.getInvokeCreatePathMatcher("/feign/context/hello", null, null, null, "GET"));
        Assertions.assertNotNull(restMethodMetadata);
    }
}
