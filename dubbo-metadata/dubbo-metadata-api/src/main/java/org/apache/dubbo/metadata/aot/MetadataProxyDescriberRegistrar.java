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
package org.apache.dubbo.metadata.aot;

import org.apache.dubbo.aot.api.JdkProxyDescriber;
import org.apache.dubbo.aot.api.ProxyDescriberRegistrar;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceV2;
import org.apache.dubbo.rpc.service.Destroyable;
import org.apache.dubbo.rpc.service.EchoService;

import java.util.ArrayList;
import java.util.List;

public class MetadataProxyDescriberRegistrar implements ProxyDescriberRegistrar {
    @Override
    public List<JdkProxyDescriber> getJdkProxyDescribers() {
        List<JdkProxyDescriber> describers = new ArrayList<>();
        describers.add(buildJdkProxyDescriber(MetadataService.class));
        describers.add(buildJdkProxyDescriber(MetadataServiceV2.class));
        return describers;
    }

    private JdkProxyDescriber buildJdkProxyDescriber(Class<?> cl) {
        List<String> proxiedInterfaces = new ArrayList<>();
        proxiedInterfaces.add(cl.getName());
        proxiedInterfaces.add(EchoService.class.getName());
        proxiedInterfaces.add(Destroyable.class.getName());
        return new JdkProxyDescriber(proxiedInterfaces, null);
    }
}
