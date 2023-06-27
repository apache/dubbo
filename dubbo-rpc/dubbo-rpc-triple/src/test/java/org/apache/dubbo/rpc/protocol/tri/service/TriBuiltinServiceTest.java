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
package org.apache.dubbo.rpc.protocol.tri.service;

import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.stub.StubSuppliers;

import io.grpc.health.v1.DubboHealthTriple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link TriBuiltinService}
 */
class TriBuiltinServiceTest {

    @Test
    void testDefaultNotEnable() {
        FrameworkModel frameworkModel = new FrameworkModel();
        TriBuiltinService triBuiltinService = new TriBuiltinService(frameworkModel);
        Assertions.assertFalse(triBuiltinService.enable());
        Assertions.assertNull(triBuiltinService.getHealthStatusManager());
    }

    @Test
    void testForceEnable(){
        FrameworkModel frameworkModel = new FrameworkModel();
        TriBuiltinService triBuiltinService = new TriBuiltinService(frameworkModel);
        triBuiltinService.init();
        String serviceName = DubboHealthTriple.SERVICE_NAME;
        Assertions.assertNotNull(triBuiltinService.getHealthStatusManager());
        PathResolver pathResolver = frameworkModel.getExtensionLoader(PathResolver.class)
            .getDefaultExtension();
        Assertions.assertNotNull(pathResolver.resolve(serviceName));
        ModuleServiceRepository repository = frameworkModel.getInternalApplicationModel()
            .getInternalModule().getServiceRepository();
        Assertions.assertFalse(repository.getAllServices().isEmpty());
        Assertions.assertNotNull(StubSuppliers.getServiceDescriptor(serviceName));
    }
}
