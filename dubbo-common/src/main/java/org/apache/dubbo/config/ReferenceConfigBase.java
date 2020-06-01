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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
     * The interface name of the reference service
     */
    protected String interfaceName;

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

    protected ServiceMetadata serviceMetadata;

    public ReferenceConfigBase() {
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
    }

    public ReferenceConfigBase(Reference reference) {
        serviceMetadata = new ServiceMetadata();
        serviceMetadata.addAttribute("ORIGIN_CONFIG", this);
        appendAnnotation(Reference.class, reference);
        setMethods(MethodConfig.constructMethodConfig(reference.methods()));
    }

    public boolean shouldCheck() {
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
        Boolean shouldInit = isInit();
        if (shouldInit == null && getConsumer() != null) {
            shouldInit = getConsumer().isInit();
        }
        if (shouldInit == null) {
            // default is true, spring will still init lazily by setting init's default value to false,
            // the def default setting happens in {@link ReferenceBean#afterPropertiesSet}.
            return true;
        }
        return shouldInit;
    }

    public void checkDefault() throws IllegalStateException {
        if (consumer == null) {
            consumer = ApplicationModel.getConfigManager()
                    .getDefaultConsumer()
                    .orElse(new ConsumerConfig());
        }
    }

    public Class<?> getActualInterface() {
        Class actualInterface = interfaceClass;
        if (interfaceClass == GenericService.class) {
            try {
                actualInterface = Class.forName(interfaceName);
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
        return actualInterface;
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (ProtocolUtils.isGeneric(getGeneric())
                || (getConsumer() != null && ProtocolUtils.isGeneric(getConsumer().getGeneric()))) {
            return GenericService.class;
        }
        try {
            if (interfaceName != null && interfaceName.length() > 0) {
                interfaceClass = Class.forName(interfaceName, true, ClassUtils.getClassLoader());
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
    @Deprecated
    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        // FIXME, add id strategy in ConfigManager
//        if (StringUtils.isEmpty(id)) {
//            id = interfaceName;
//        }
    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? null : interfaceClass.getName());
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

    @Override
    @Parameter(excluded = true)
    public String getPrefix() {
        return DUBBO + ".reference." + interfaceName;
    }

    public void resolveFile() {
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
                Properties properties = new Properties();
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
        super.computeValidRegistryIds();
        if (StringUtils.isEmpty(getRegistryIds())) {
            if (getConsumer() != null && StringUtils.isNotEmpty(getConsumer().getRegistryIds())) {
                setRegistryIds(getConsumer().getRegistryIds());
            }
        }
    }

    @Parameter(excluded = true)
    public String getUniqueServiceName() {
        return URL.buildKey(interfaceName, getGroup(), getVersion());
    }

    @Override
    public String getVersion() {
        return StringUtils.isEmpty(this.version) ? (consumer != null ? consumer.getVersion() : this.version) : this.version;
    }

    @Override
    public String getGroup() {
        return StringUtils.isEmpty(this.group) ? (consumer != null ? consumer.getGroup() : this.group) : this.group;
    }

    public abstract T get();

    public abstract void destroy();


}
