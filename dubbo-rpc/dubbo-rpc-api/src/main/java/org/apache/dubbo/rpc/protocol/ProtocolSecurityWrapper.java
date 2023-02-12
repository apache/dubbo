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
package org.apache.dubbo.rpc.protocol;

import java.util.List;
import java.util.Optional;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.SerializeSecurityConfigurator;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.model.ServiceModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

@Activate(order = 200)
public class ProtocolSecurityWrapper implements Protocol {
    private final Protocol protocol;
    
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ProtocolSecurityWrapper.class);

    public ProtocolSecurityWrapper(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    @Override
    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        try {
            ServiceModel serviceModel = invoker.getUrl().getServiceModel();
            ScopeModel scopeModel = invoker.getUrl().getScopeModel();
            SerializeSecurityConfigurator serializeSecurityConfigurator = ScopeModelUtil.getModuleModel(scopeModel)
                .getBeanFactory().getBean(SerializeSecurityConfigurator.class);
            serializeSecurityConfigurator.refreshStatus();
            serializeSecurityConfigurator.refreshCheck();

            Optional.ofNullable(serviceModel)
                .map(ServiceModel::getServiceModel)
                .map(ServiceDescriptor::getServiceInterfaceClass)
                .ifPresent(serializeSecurityConfigurator::registerInterface);

            Optional.ofNullable(serviceModel)
                .map(ServiceModel::getServiceMetadata)
                .map(ServiceMetadata::getServiceType)
                .ifPresent(serializeSecurityConfigurator::registerInterface);
        } catch (Throwable t) {
            logger.error(INTERNAL_ERROR, "", "", "Failed to register interface for security check", t);
        }
        return protocol.export(invoker);
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        try {
            ServiceModel serviceModel = url.getServiceModel();
            ScopeModel scopeModel = url.getScopeModel();
            SerializeSecurityConfigurator serializeSecurityConfigurator = ScopeModelUtil.getModuleModel(scopeModel)
                .getBeanFactory().getBean(SerializeSecurityConfigurator.class);
            serializeSecurityConfigurator.refreshStatus();
            serializeSecurityConfigurator.refreshCheck();

            Optional.ofNullable(serviceModel)
                .map(ServiceModel::getServiceModel)
                .map(ServiceDescriptor::getServiceInterfaceClass)
                .ifPresent(serializeSecurityConfigurator::registerInterface);

            Optional.ofNullable(serviceModel)
                .map(ServiceModel::getServiceMetadata)
                .map(ServiceMetadata::getServiceType)
                .ifPresent(serializeSecurityConfigurator::registerInterface);
            serializeSecurityConfigurator.registerInterface(type);
        } catch (Throwable t) {
            logger.error(INTERNAL_ERROR, "", "", "Failed to register interface for security check", t);
        }

        return protocol.refer(type, url);
    }

    @Override
    public void destroy() {
        protocol.destroy();
    }

    @Override
    public List<ProtocolServer> getServers() {
        return protocol.getServers();
    }
}
