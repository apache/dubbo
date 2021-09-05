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
    private Environment environment;
    private ConfigManager configManager;
    private ServiceRepository serviceRepository;

    private FrameworkModel frameworkModel;

    private ModuleModel internalModule;


    // --------- static methods ----------//

    public static ApplicationModel ofNullable(ApplicationModel applicationModel) {
        if (applicationModel != null) {
            return applicationModel;
        } else {
            return defaultModel();
        }
    }

    public static ApplicationModel defaultModel() {
        if (defaultInstance == null) {
            synchronized (ApplicationModel.class) {
                if (defaultInstance == null) {
                    defaultInstance = new ApplicationModel(FrameworkModel.defaultModel());
                }
            }
        }
        return defaultInstance;
    }

    @Deprecated
    public static Collection<ConsumerModel> allConsumerModels() {
        return defaultModel().getApplicationServiceRepository().allConsumerModels();
    }

    @Deprecated
    public static Collection<ProviderModel> allProviderModels() {
        return defaultModel().getApplicationServiceRepository().allProviderModels();
    }

    @Deprecated
    public static ProviderModel getProviderModel(String serviceKey) {
        return defaultModel().getDefaultModule().getServiceRepository().lookupExportedService(serviceKey);
    }

    @Deprecated
    public static ConsumerModel getConsumerModel(String serviceKey) {
        return defaultModel().getDefaultModule().getServiceRepository().lookupReferredService(serviceKey);
    }

    @Deprecated
    public static Environment getEnvironment() {
        return defaultModel().getApplicationEnvironment();
    }

    @Deprecated
    public static ConfigManager getConfigManager() {
        return defaultModel().getApplicationConfigManager();
    }

    @Deprecated
    public static ServiceRepository getServiceRepository() {
        return defaultModel().getApplicationServiceRepository();
    }

    @Deprecated
    public static ExecutorRepository getExecutorRepository() {
        return defaultModel().getApplicationExecutorRepository();
    }

    @Deprecated
    public static ApplicationConfig getApplicationConfig() {
        return defaultModel().getCurrentConfig();
    }

    @Deprecated
    public static String getName() {
        return defaultModel().getCurrentConfig().getName();
    }

    @Deprecated
    public static String getApplication() {
        return getName();
    }

    // only for unit test
    @Deprecated
    public static void reset() {
        if (defaultInstance != null) {
            defaultInstance.destroy();
            defaultInstance = null;
        }
    }

    // ------------- instance methods ---------------//

    public ApplicationModel(FrameworkModel frameworkModel) {
        super(frameworkModel, ExtensionScope.APPLICATION);
        this.frameworkModel = frameworkModel;
        frameworkModel.addApplication(this);
        initialize();
    }

    protected void initialize() {
        super.initialize();
        internalModule = new ModuleModel(this);
        this.serviceRepository = new ServiceRepository(this);

        ExtensionLoader<ApplicationInitListener> extensionLoader = this.getExtensionLoader(ApplicationInitListener.class);
        Set<String> listenerNames = extensionLoader.getSupportedExtensions();
        for (String listenerName : listenerNames) {
            extensionLoader.getExtension(listenerName).init();
        }

        initFrameworkExts();

        postProcessAfterCreated();
    }

    private void initFrameworkExts() {
        Set<FrameworkExt> exts = this.getExtensionLoader(FrameworkExt.class).getSupportedExtensionInstances();
        for (FrameworkExt ext : exts) {
            ext.initialize();
        }
    }

    @Override
    public void destroy() {
        // TODO destroy application resources
        for (ModuleModel moduleModel : new ArrayList<>(moduleModels)) {
            moduleModel.destroy();
        }
        frameworkModel.removeApplication(this);
        if (environment != null) {
            environment.destroy();
            environment = null;
        }
        if (configManager != null) {
            configManager.destroy();
            configManager = null;
        }
        if (serviceRepository != null) {
            serviceRepository.destroy();
            serviceRepository = null;
        }
    }

    public FrameworkModel getFrameworkModel() {
        return frameworkModel;
    }

    public Environment getApplicationEnvironment() {
        if (environment == null) {
            environment = (Environment) this.getExtensionLoader(FrameworkExt.class)
                .getExtension(Environment.NAME);
        }
        return environment;
    }

    public ConfigManager getApplicationConfigManager() {
        if (configManager == null) {
            configManager = (ConfigManager) this.getExtensionLoader(FrameworkExt.class)
                .getExtension(ConfigManager.NAME);
        }
        return configManager;
    }

    public ServiceRepository getApplicationServiceRepository() {
        return serviceRepository;
    }

    public ExecutorRepository getApplicationExecutorRepository() {
        return this.getExtensionLoader(ExecutorRepository.class).getDefaultExtension();
    }

    public ApplicationConfig getCurrentConfig() {
        return getApplicationConfigManager().getApplicationOrElseThrow();
    }

    public String getApplicationName() {
        return getCurrentConfig().getName();
    }

    public synchronized void addModule(ModuleModel model) {
        if (!this.moduleModels.contains(model)) {
            this.moduleModels.add(model);
        }
    }

    public synchronized void removeModule(ModuleModel model) {
        this.moduleModels.remove(model);
    }

    public List<ModuleModel> getModuleModels() {
        return moduleModels;
    }

    public synchronized ModuleModel getDefaultModule() {
        for (ModuleModel moduleModel : moduleModels) {
            if (moduleModel != internalModule) {
                return moduleModel;
            }
        }
        ModuleModel moduleModel = new ModuleModel(this);
        this.addModule(moduleModel);
        return moduleModel;
    }

    public ModuleModel getInternalModule() {
        return internalModule;
    }

    @Deprecated
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Deprecated
    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Deprecated
    public void setServiceRepository(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    public String toString() {
        return "ApplicationModel";
    }
}
