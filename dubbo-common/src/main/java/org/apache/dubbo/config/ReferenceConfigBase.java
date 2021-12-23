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
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.RegexProperties;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;

/**
 * ReferenceConfig
 *
 * @export
 */
public abstract class ReferenceConfigBase<T> extends AbstractReferenceConfig {

    private static final long serialVersionUID = -5864351140409987595L;

    /**
     * The interface class of the reference service
     */
    protected Class<?> interfaceClass;

    /**
     * client type
     */
    protected String client;

    /**
     * The url for peer-to-peer invocation
     */
    protected String url;

    /**
     * The consumer config (default)
     */
    protected ConsumerConfig consumer;

    /**
     * Only the service provider of the specified protocol is invoked, and other protocols are ignored.
     */
    protected String protocol;


    public ReferenceConfigBase() {
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
    }

    public ReferenceConfigBase(ModuleModel moduleModel) {
        super(moduleModel);
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
    }

    public ReferenceConfigBase(Reference reference) {
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
        appendAnnotation(Reference.class, reference);
        setMethods(MethodConfig.constructMethodConfig(reference.methods()));
    }

    public ReferenceConfigBase(ModuleModel moduleModel, Reference reference) {
        super(moduleModel);
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
        appendAnnotation(Reference.class, reference);
        setMethods(MethodConfig.constructMethodConfig(reference.methods()));
    }

    public boolean shouldCheck() {
        checkDefault();
        Boolean shouldCheck = isCheck();
        if (shouldCheck == null && getConsumer() != null) {
            shouldCheck = getConsumer().isCheck();
        }
        if (shouldCheck == null) {
            // default true
            shouldCheck = true;
        }
        return shouldCheck;
    }

    public boolean shouldInit() {
        checkDefault();
        Boolean shouldInit = isInit();
        if (shouldInit == null && getConsumer() != null) {
            shouldInit = getConsumer().isInit();
        }
        if (shouldInit == null) {
            // default is true
            return true;
        }
        return shouldInit;
    }

    @Override
    protected void preProcessRefresh() {
        super.preProcessRefresh();
        if (consumer == null) {
            consumer = getModuleConfigManager()
                    .getDefaultConsumer()
                    .orElseThrow(() -> new IllegalStateException("Default consumer is not initialized"));
        }
    }

    @Override
    @Parameter(excluded = true, attribute = false)
    public List<String> getPrefixes() {
        List<String> prefixes = new ArrayList<>();
        // dubbo.reference.{interface-name}
        prefixes.add(DUBBO + ".reference." + interfaceName);
        return prefixes;
    }

    @Override
    public Map<String, String> getMetaData() {
        Map<String, String> metaData = new HashMap<>();
        ConsumerConfig consumer = this.getConsumer();
        // consumer should be initialized at preProcessRefresh()
        if (isRefreshed() && consumer == null) {
            throw new IllegalStateException("Consumer is not initialized");
        }
        // use consumer attributes as default value
        appendAttributes(metaData, consumer);
        appendAttributes(metaData, this);
        return metaData;
    }

    /**
     * Get service interface class of this reference.
     * The actual service type of remote provider.
     * @return
     */
    public Class<?> getServiceInterfaceClass() {
        Class<?> actualInterface = interfaceClass;
        if (interfaceClass == GenericService.class) {
            try {
                if(getInterfaceClassLoader() != null) {
                    actualInterface = Class.forName(interfaceName, false, getInterfaceClassLoader());
                } else {
                    actualInterface = Class.forName(interfaceName);
                }
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        return actualInterface;
    }

    /**
     * Get proxy interface class of this reference.
     * The proxy interface class is used to create proxy instance.
     * @return
     */
    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }

        String generic = getGeneric();
        if (StringUtils.isBlank(generic) && getConsumer() != null) {
            generic = getConsumer().getGeneric();
        }
        if(getInterfaceClassLoader() != null) {
            interfaceClass = determineInterfaceClass(generic, interfaceName, getInterfaceClassLoader());
        } else {
            interfaceClass = determineInterfaceClass(generic, interfaceName);
        }
        return interfaceClass;
    }

    /**
     * Determine the interface of the proxy class
     * @param generic
     * @param interfaceName
     * @return
     */
    public static Class<?> determineInterfaceClass(String generic, String interfaceName) {
        return determineInterfaceClass(generic, interfaceName, ClassUtils.getClassLoader());
    }

    public static Class<?> determineInterfaceClass(String generic, String interfaceName, ClassLoader classLoader) {
        if (ProtocolUtils.isGeneric(generic)) {
            return GenericService.class;
        }
        try {
            if (StringUtils.isNotEmpty(interfaceName)) {
                return Class.forName(interfaceName, true, classLoader);
            }
        } catch (ClassNotFoundException t) {
            throw new IllegalStateException(t.getMessage(), t);
        }
        return null;
    }

    @Override
    protected void postProcessAfterScopeModelChanged(ScopeModel oldScopeModel, ScopeModel newScopeModel) {
        super.postProcessAfterScopeModelChanged(oldScopeModel, newScopeModel);
        if (this.consumer != null && this.consumer.getScopeModel() != scopeModel) {
            this.consumer.setScopeModel(scopeModel);
        }
    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        setInterface(interfaceClass == null ? null : interfaceClass.getName());
        if (getInterfaceClassLoader() == null) {
            setInterfaceClassLoader(interfaceClass == null ? null : interfaceClass.getClassLoader());
        }
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    @Parameter(excluded = true)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ConsumerConfig getConsumer() {
        return consumer;
    }

    public void setConsumer(ConsumerConfig consumer) {
        this.consumer = consumer;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
    }

    protected void resolveFile() {
        String resolve = System.getProperty(interfaceName);
        String resolveFile = null;
        if (StringUtils.isEmpty(resolve)) {
            resolveFile = System.getProperty("dubbo.resolve.file");
            if (StringUtils.isEmpty(resolveFile)) {
                File userResolveFile = new File(new File(System.getProperty("user.home")), "dubbo-resolve.properties");
                if (userResolveFile.exists()) {
                    resolveFile = userResolveFile.getAbsolutePath();
                }
            }
            if (resolveFile != null && resolveFile.length() > 0) {
                Properties properties = new RegexProperties();
                try (FileInputStream fis = new FileInputStream(new File(resolveFile))) {
                    properties.load(fis);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to load " + resolveFile + ", cause: " + e.getMessage(), e);
                }

                resolve = properties.getProperty(interfaceName);
            }
        }
        if (resolve != null && resolve.length() > 0) {
            url = resolve;
            if (logger.isWarnEnabled()) {
                if (resolveFile != null) {
                    logger.warn("Using default dubbo resolve file " + resolveFile + " replace " + interfaceName + "" + resolve + " to p2p invoke remote service.");
                } else {
                    logger.warn("Using -D" + interfaceName + "=" + resolve + " to p2p invoke remote service.");
                }
            }
        }
    }

    @Override
    protected void computeValidRegistryIds() {
        if (consumer != null) {
            if (notHasSelfRegistryProperty()) {
                setRegistries(consumer.getRegistries());
                setRegistryIds(consumer.getRegistryIds());
            }
        }
        super.computeValidRegistryIds();
    }

    @Parameter(excluded = true, attribute = false)
    public String getUniqueServiceName() {
        return interfaceName != null ? URL.buildKey(interfaceName, getGroup(), getVersion()) : null;
    }

    @Override
    public String getVersion() {
        return StringUtils.isEmpty(this.version) ? (consumer != null ? consumer.getVersion() : this.version) : this.version;
    }

    @Override
    public String getGroup() {
        return StringUtils.isEmpty(this.group) ? (consumer != null ? consumer.getGroup() : this.group) : this.group;
    }

    public Boolean shouldReferAsync() {
        Boolean shouldReferAsync = getReferAsync();
        if (shouldReferAsync == null) {
            shouldReferAsync = consumer != null && consumer.getReferAsync() != null && consumer.getReferAsync();
        }

        return shouldReferAsync;
    }

    public abstract T get();

    public void destroy() {
        getModuleConfigManager().removeConfig(this);
    }

}
