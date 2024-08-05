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
package org.apache.dubbo.spring.boot.actuate.endpoint.metadata;

import org.apache.dubbo.rpc.GracefulShutdown;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Dubbo Graceful Shutdown
 *
 * @since 3.3.0
 */
@Component
public class DubboGracefulShutdownMetadata {

    @Autowired
    private ApplicationModel applicationModel;

    @Autowired
    private DubboOfflineMetadata dubboOfflineMetadata;

    public Map<String, Object> gracefulShutdown() {
        for (GracefulShutdown gracefulShutdown :
                applicationModel.getBeanFactory().getBeansOfType(GracefulShutdown.class)) {
            gracefulShutdown.readonly();
        }
        return dubboOfflineMetadata.offline(".*");
    }
}
