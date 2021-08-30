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
package org.apache.dubbo.rpc.model;

import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.context.FrameworkExt;
import org.apache.dubbo.common.extension.ExtensionDirector;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.context.ConfigManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link ExtensionLoader}, {@code DubboBootstrap} and this class are at present designed to be
 * singleton or static (by itself totally static or uses some static fields). So the instances
 * returned from them are of process scope. If you want to support multiple dubbo servers in one
 * single process, you may need to refactor those three classes.
 * <p>
 * Represent a application which is using Dubbo and store basic metadata info for using
 * during the processing of RPC invoking.
 * <p>
 * ApplicationModel includes many ProviderModel which is about published services
 * and many Consumer Model which is about subscribed services.
 * <p>
 */

public class ApplicationModel extends ScopeModel {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ApplicationModel.class);
    public static final String NAME = "application";
    private static volatile ApplicationModel defaultInstance;

    private volatile List<ModuleModel> moduleModels = Collections.synchronizedList(new ArrayList<>());
    private AtomicBoolean initFlag = new AtomicBoolean(false);
    private Environment environment;
    private ConfigManager configManager;
    private ServiceRepository serviceRepository;

    private FrameworkModel frameworkModel;

    public ApplicationModel(FrameworkModel frameworkModel) {
        this(frameworkModel, true);
    }

    public ApplicationModel(FrameworkModel frameworkModel, boolean shouldInit) {
        super(frameworkModel, new ExtensionDirector(frameworkModel.getExtensionDirector(), ExtensionScope.APPLICATION));
        this.frameworkModel = frameworkModel;
        frameworkModel.addApplication(this);

        if (shouldInit) {
            postConstruct();
        }
    }

    public static ApplicationModel defaultModel() {
        if (defaultInstance == null || !defaultInstance.isReady()) {
            synchronized (ApplicationModel.class) {
                if (defaultInstance == null) {
                    defaultInstance = new ApplicationModel(FrameworkModel.defaultModel(), false);
                }
                if (!defaultInstance.isReady()) {
                    defaultInstance.postConstruct();
                }
            }
        }
        return defaultInstance;
    }

    public void init() {
        if (initFlag.compareAndSet(false, true)) {
            ExtensionLoader<ApplicationInitListener> extensionLoader = this.getExtensionLoader(ApplicationInitListener.class);
            Set<String> listenerNames = extensionLoader.getSupportedExtensions();
            for (String listenerName : listenerNames) {
                extensionLoader.getExtension(listenerName).init();
            }
        }
    }

    public void destroy() {
        // TODO destroy application resources
    }

    public FrameworkModel getFrameworkModel() {
        return frameworkModel;
    }

    @Deprecated
    public static Collection<ConsumerModel> allConsumerModels() {
        return defaultModel().getApplicationServiceRepository().getReferredServices();
    }

    @Deprecated
    public static Collection<ProviderModel> allProviderModels() {
        return defaultModel().getApplicationServiceRepository().getExportedServices();
    }

    @Deprecated
    public static ProviderModel getProviderModel(String serviceKey) {
        return defaultModel().getApplicationServiceRepository().lookupExportedService(serviceKey);
    }

    @Deprecated
    public static ConsumerModel getConsumerModel(String serviceKey) {
        return defaultModel().getApplicationServiceRepository().lookupReferredService(serviceKey);
    }

    public void initFrameworkExts() {
        Set<FrameworkExt> exts = this.getExtensionLoader(FrameworkExt.class).getSupportedExtensionInstances();
        for (FrameworkExt ext : exts) {
            ext.initialize();
        }
    }

    @Deprecated
    public static Environment getEnvironment() {
        return defaultModel().getApplicationEnvironment();
    }

    public Environment getApplicationEnvironment() {
        if (environment == null) {
            environment = (Environment) this.getExtensionLoader(FrameworkExt.class)
                .getExtension(Environment.NAME);
        }
        return environment;
    }

    @Deprecated
    public static ConfigManager getConfigManager() {
        return defaultModel().getApplicationConfigManager();
    }

    public ConfigManager getApplicationConfigManager() {
        if (configManager == null) {
            configManager = (ConfigManager) this.getExtensionLoader(FrameworkExt.class)
                .getExtension(ConfigManager.NAME);
        }
        return configManager;
    }

    @Deprecated
    public static ServiceRepository getServiceRepository() {
        return defaultModel().getApplicationServiceRepository();
    }

    public ServiceRepository getApplicationServiceRepository() {
        if (serviceRepository == null) {
            serviceRepository = (ServiceRepository) this.getExtensionLoader(FrameworkExt.class)
                .getExtension(ServiceRepository.NAME);
        }
        return serviceRepository;
    }

    @Deprecated
    public static ExecutorRepository getExecutorRepository() {
        return defaultModel().getApplicationExecutorRepository();
    }

    public ExecutorRepository getApplicationExecutorRepository() {
        return this.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
    }

    @Deprecated
    public static ApplicationConfig getApplicationConfig() {
        return defaultModel().getCurrentConfig();
    }

    public ApplicationConfig getCurrentConfig() {
        return getApplicationConfigManager().getApplicationOrElseThrow();
    }

    @Deprecated
    public static String getName() {
        return defaultModel().getCurrentConfig().getName();
    }

    @Deprecated
    public static String getApplication() {
        return getName();
    }

    public String getApplicationName() {
        return getCurrentConfig().getName();
    }

    public void addModule(ModuleModel model) {
        if (!this.moduleModels.contains(model)) {
            this.moduleModels.add(model);
        }
    }

    public void removeModule(ModuleModel model) {
        this.moduleModels.remove(model);
    }

    public List<ModuleModel> getModuleModels() {
        return moduleModels;
    }

    public synchronized ModuleModel getDefaultModule() {
        if (moduleModels.isEmpty()) {
            this.addModule(new ModuleModel(this));
        }
        return moduleModels.get(0);
    }

    // only for unit test
    @Deprecated
    public static void reset() {
        if (defaultInstance != null) {
            defaultInstance.destroy();
            defaultInstance = null;
        }
    }

}
