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
package org.apache.dubbo.xds.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.xds.security.NativeTlsAdapter;
import org.apache.dubbo.xds.traffic.XdsAdmissionInvoker;
import org.apache.dubbo.xds.traffic.XdsMetadataFaultInvoker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Direct invocation never enters a Registry/Directory or consults a revision cache. */
final class PointToPointMetadataFetcher implements MetadataFetcher {
    private final ApplicationModel application;

    PointToPointMetadataFetcher(ApplicationModel application) {
        this.application = application;
    }

    @Override
    public MetadataInfo fetch(XdsEndpoint business, XdsEndpoint endpoint, MetadataEntryOptions options) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("interface", MetadataService.class.getName());
        parameters.put("group", options.group);
        parameters.put("version", MetadataService.VERSION);
        parameters.put("timeout", Integer.toString(options.timeoutMillis));
        parameters.put("retries", "0");
        parameters.put("check", "false");
        parameters.put("side", "consumer");
        parameters.put("reference.filter", "-default");
        parameters.put("application", application.getApplicationName());
        URL url = new URL(
                        options.protocol,
                        endpoint.getHost(),
                        endpoint.getPort(),
                        MetadataService.class.getName(),
                        parameters)
                .setScopeModel(application.getInternalModule());
        url = url.putAttribute(XdsEndpoint.ATTRIBUTE, endpoint);
        url = NativeTlsAdapter.decorate(url);
        ConsumerModel model = application
                .getInternalModule()
                .registerInternalConsumer(
                        MetadataService.class,
                        url,
                        application.getInternalModule().getServiceRepository().registerService(MetadataService.class));
        url = url.setServiceModel(model);
        Protocol protocol = application.getExtensionLoader(Protocol.class).getExtension(options.protocol, false);
        Invoker<MetadataService> invoker = null;
        try {
            invoker = protocol.refer(MetadataService.class, url);
            if ("tri".equals(options.protocol)) {
                invoker = new XdsMetadataFaultInvoker<>(new XdsAdmissionInvoker<>(invoker, endpoint), endpoint);
            }
            MetadataService service = application
                    .getExtensionLoader(ProxyFactory.class)
                    .getAdaptiveExtension()
                    .getProxy(invoker);
            MetadataInfo result = new MetadataInfo(options.application);
            List<MetadataInfo> infos = service.getMetadataInfos();
            if (infos == null) {
                throw new IllegalStateException("MetadataService V1 returned no metadata response");
            }
            for (MetadataInfo info : infos) {
                if (info == null) {
                    continue;
                }
                for (MetadataInfo.ServiceInfo exported : info.getServices().values()) {
                    if (business.getTarget().getProtocol().equals(exported.getProtocol())
                            && business.getPort() == exported.getPort()) {
                        result.getServices().put(exported.getMatchKey(), exported);
                    }
                }
            }
            if (infos.isEmpty()) {
                for (String exported : service.getExportedURLs()) {
                    URL provider = URL.valueOf(exported).setScopeModel(application);
                    if (business.getTarget().getProtocol().equals(provider.getProtocol())
                            && business.getPort() == provider.getPort()) {
                        result.addService(provider);
                    }
                }
            }
            result.init();
            return result;
        } finally {
            if (invoker != null) {
                invoker.destroy();
            }
            application.getInternalModule().getServiceRepository().unregisterConsumer(model);
        }
    }
}
