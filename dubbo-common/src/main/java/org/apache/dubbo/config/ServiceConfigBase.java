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
package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;

/**
 * ServiceConfig
 *
 * @export
 */
public abstract class ServiceConfigBase<T> extends AbstractServiceConfig {

    private static final long serialVersionUID = 3033787999037024738L;



    /**
     * The interface class of the exported service
     */
    protected Class<?> interfaceClass;

    /**
     * The reference of the interface implementation
     */
    protected T ref;

    /**
     * The service name
     */
    protected String path;

    /**
     * The provider configuration
     */
    protected ProviderConfig provider;

    /**
     * The providerIds
     */
    protected String providerIds;

    /**
     * whether it is a GenericService
     */
    protected volatile String generic;


    public ServiceConfigBase() {
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
    }

    public ServiceConfigBase(ModuleModel moduleModel) {
        super(moduleModel);
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
    }

    public ServiceConfigBase(Service service) {
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
        appendAnnotation(Service.class, service);
        setMethods(MethodConfig.constructMethodConfig(service.methods()));
    }

    public ServiceConfigBase(ModuleModel moduleModel, Service service) {
        super(moduleModel);
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
        appendAnnotation(Service.class, service);
        setMethods(MethodConfig.constructMethodConfig(service.methods()));
    }

    @Deprecated
    private static List<ProtocolConfig> convertProviderToProtocol(List<ProviderConfig> providers) {
        if (CollectionUtils.isEmpty(providers)) {
            return null;
        }
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>(providers.size());
        for (ProviderConfig provider : providers) {
            protocols.add(convertProviderToProtocol(provider));
        }
        return protocols;
    }

    @Override
    protected void postProcessAfterScopeModelChanged(ScopeModel oldScopeModel, ScopeModel newScopeModel) {
        super.postProcessAfterScopeModelChanged(oldScopeModel, newScopeModel);
        if (this.provider != null && this.provider.getScopeModel() != scopeModel) {
            this.provider.setScopeModel(scopeModel);
        }
    }

    @Deprecated
    private static List<ProviderConfig> convertProtocolToProvider(List<ProtocolConfig> protocols) {
        if (CollectionUtils.isEmpty(protocols)) {
            return null;
        }
        List<ProviderConfig> providers = new ArrayList<ProviderConfig>(protocols.size());
        for (ProtocolConfig provider : protocols) {
            providers.add(convertProtocolToProvider(provider));
        }
        return providers;
    }

    @Deprecated
    private static ProtocolConfig convertProviderToProtocol(ProviderConfig provider) {
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName(provider.getProtocol().getName());
        protocol.setServer(provider.getServer());
        protocol.setClient(provider.getClient());
        protocol.setCodec(provider.getCodec());
        protocol.setHost(provider.getHost());
        protocol.setPort(provider.getPort());
        protocol.setPath(provider.getPath());
        protocol.setPayload(provider.getPayload());
        protocol.setThreads(provider.getThreads());
        protocol.setParameters(provider.getParameters());
        return protocol;
    }

    @Deprecated
    private static ProviderConfig convertProtocolToProvider(ProtocolConfig protocol) {
        ProviderConfig provider = new ProviderConfig();
        provider.setProtocol(protocol);
        provider.setServer(protocol.getServer());
        provider.setClient(protocol.getClient());
        provider.setCodec(protocol.getCodec());
        provider.setHost(protocol.getHost());
        provider.setPort(protocol.getPort());
        provider.setPath(protocol.getPath());
        provider.setPayload(protocol.getPayload());
        provider.setThreads(protocol.getThreads());
        provider.setParameters(protocol.getParameters());
        return provider;
    }

    public boolean shouldExport() {
        Boolean export = getExport();
        // default value is true
        return export == null ? true : export;
    }

    @Override
    public Boolean getExport() {
        return (export == null && provider != null) ? provider.getExport() : export;
    }

    public boolean shouldDelay() {
        Integer delay = getDelay();
        return delay != null && delay > 0;
    }

    @Override
    public Integer getDelay() {
        return (delay == null && provider != null) ? provider.getDelay() : delay;
    }

    protected void checkRef() {
        // reference should not be null, and is the implementation of the given interface
        if (ref == null) {
            throw new IllegalStateException("ref not allow null!");
        }
        if (!interfaceClass.isInstance(ref)) {
            throw new IllegalStateException("The class "
                + getClassDesc(ref.getClass()) + " unimplemented interface "
                + getClassDesc(interfaceClass) + "!");
        }
    }

    private String getClassDesc(Class clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        return clazz.getName() + "[classloader=" + classLoader.getClass().getName() + "@" + classLoader.hashCode() + "]";
    }

    public Optional<String> getContextPath(ProtocolConfig protocolConfig) {
        String contextPath = protocolConfig.getContextpath();
        if (StringUtils.isEmpty(contextPath) && provider != null) {
            contextPath = provider.getContextpath();
        }
        return Optional.ofNullable(contextPath);
    }

    protected Class getServiceClass(T ref) {
        return ref.getClass();
    }

    @Override
    protected void preProcessRefresh() {
        super.preProcessRefresh();
        convertProviderIdToProvider();
        if (provider == null) {
            provider = getModuleConfigManager()
                    .getDefaultProvider()
                    .orElseThrow(() -> new IllegalStateException("Default provider is not initialized"));
        }
    }

    @Override
    public Map<String, String> getMetaData() {
        Map<String, String> metaData = new HashMap<>();
        ProviderConfig provider = this.getProvider();
        // provider should be inited at preProcessRefresh()
        if (isRefreshed() && provider == null) {
            throw new IllegalStateException("Provider is not initialized");
        }
        // use provider attributes as default value
        appendAttributes(metaData, provider);
        // Finally, put the service's attributes, overriding previous attributes
        appendAttributes(metaData, this);
        return metaData;
    }

    protected void checkProtocol() {
        if (provider != null && notHasSelfProtocolProperty()) {
            setProtocols(provider.getProtocols());
            setProtocolIds(provider.getProtocolIds());
        }
        convertProtocolIdsToProtocols();
    }

    private boolean notHasSelfProtocolProperty() {
        return CollectionUtils.isEmpty(protocols) && StringUtils.isEmpty(protocolIds);
    }

    protected void completeCompoundConfigs() {
        super.completeCompoundConfigs(provider);
        if (provider != null) {
            if (notHasSelfProtocolProperty()) {
                setProtocols(provider.getProtocols());
                setProtocolIds(provider.getProtocolIds());
            }
            if (configCenter == null) {
                setConfigCenter(provider.getConfigCenter());
            }
        }
    }

    protected void convertProviderIdToProvider() {
        if (provider == null && StringUtils.hasText(providerIds)) {
            provider = getModuleConfigManager().getProvider(providerIds)
                    .orElseThrow(() -> new IllegalStateException("Provider config not found: " + providerIds));
        }
    }

    protected void convertProtocolIdsToProtocols() {
        if (StringUtils.isEmpty(protocolIds)) {
            if (CollectionUtils.isEmpty(protocols)) {
                List<ProtocolConfig> protocolConfigs = getConfigManager().getDefaultProtocols();
                if (protocolConfigs.isEmpty()) {
                    throw new IllegalStateException("The default protocol has not been initialized.");
                }
                setProtocols(protocolConfigs);
            }
        } else {
            String[] idsArray = COMMA_SPLIT_PATTERN.split(protocolIds);
            Set<String> idsSet = new LinkedHashSet<>(Arrays.asList(idsArray));
            List<ProtocolConfig> tmpProtocols = new ArrayList<>();
            for (String id : idsSet) {
                Optional<ProtocolConfig> globalProtocol = getConfigManager().getProtocol(id);
                if (globalProtocol.isPresent()) {
                    tmpProtocols.add(globalProtocol.get());
                } else {
                    throw new IllegalStateException("Protocol not found: "+id);
                }
            }
            setProtocols(tmpProtocols);
        }
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (ref instanceof GenericService) {
            return GenericService.class;
        }
        try {
            if (StringUtils.isNotEmpty(interfaceName)) {
                this.interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                        .getContextClassLoader());
            }
        } catch (ClassNotFoundException t) {
            throw new IllegalStateException(t.getMessage(), t);
        }
        return interfaceClass;
    }

    /**
     * @param interfaceClass
     * @see #setInterface(Class)
     * @deprecated
     */
    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }



    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? null : interfaceClass.getName());
        if (getInterfaceClassLoader() == null) {
            setInterfaceClassLoader(interfaceClass == null ? null : interfaceClass.getClassLoader());
        }
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }

    @Parameter(excluded = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ProviderConfig getProvider() {
        return provider;
    }

    public void setProvider(ProviderConfig provider) {
        getModuleConfigManager().addProvider(provider);
        this.provider = provider;
    }

    @Parameter(excluded = true)
    public String getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(String providerIds) {
        this.providerIds = providerIds;
    }

    public String getGeneric() {
        return generic;
    }

    public void setGeneric(String generic) {
        if (StringUtils.isEmpty(generic)) {
            return;
        }
        if (ProtocolUtils.isValidGenericValue(generic)) {
            this.generic = generic;
        } else {
            throw new IllegalArgumentException("Unsupported generic type " + generic);
        }
    }

//    @Override
//    public void setMock(String mock) {
//        throw new IllegalArgumentException("mock doesn't support on provider side");
//    }
//
//    @Override
//    public void setMock(Object mock) {
//        throw new IllegalArgumentException("mock doesn't support on provider side");
//    }

    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
    }

//    /**
//     * @deprecated Replace to getProtocols()
//     */
//    @Deprecated
//    public List<ProviderConfig> getProviders() {
//        return convertProtocolToProvider(protocols);
//    }
//
//    /**
//     * @deprecated Replace to setProtocols()
//     */
//    @Deprecated
//    public void setProviders(List<ProviderConfig> providers) {
//        this.protocols = convertProviderToProtocol(providers);
//    }

    @Override
    @Parameter(excluded = true, attribute = false)
    public List<String> getPrefixes() {
        List<String> prefixes = new ArrayList<>();
        // dubbo.service.{interface-name}
        prefixes.add(DUBBO + ".service." + interfaceName);
        return prefixes;
    }

    @Parameter(excluded = true, attribute = false)
    public String getUniqueServiceName() {
        return interfaceName != null ? URL.buildKey(interfaceName, getGroup(), getVersion()) : null;
    }

    @Override
    public String getGroup() {
        return StringUtils.isEmpty(this.group) ? (provider != null ? provider.getGroup() : this.group) : this.group;
    }

    @Override
    public String getVersion() {
        return StringUtils.isEmpty(this.version) ? (provider != null ? provider.getVersion() : this.version) : this.version;
    }

    @Override
    protected void computeValidRegistryIds() {
        if (provider != null && notHasSelfRegistryProperty()) {
            setRegistries(provider.getRegistries());
            setRegistryIds(provider.getRegistryIds());
        }
        super.computeValidRegistryIds();
    }

    public Boolean shouldExportAsync() {
        Boolean shouldExportAsync = getExportAsync();
        if (shouldExportAsync == null) {
            shouldExportAsync = provider != null && provider.getExportAsync() != null && provider.getExportAsync();
        }

        return shouldExportAsync;
    }

    /**
     * export service and auto start application instance
     */
    public abstract void export();

    public abstract void unexport();

    public abstract boolean isExported();

    public abstract boolean isUnexported();

}
