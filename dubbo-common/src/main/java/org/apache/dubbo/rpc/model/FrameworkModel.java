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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.resource.GlobalResourcesRepository;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.definition.TypeDefinitionBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Model of dubbo framework, it can be shared with multiple applications.
 */
public class FrameworkModel extends ScopeModel {

    protected static final Logger LOGGER = LoggerFactory.getLogger(FrameworkModel.class);

    public static final String NAME = "FrameworkModel";
    private static final AtomicLong index = new AtomicLong(1);
    // internal app index is 0, default app index is 1
    private final AtomicLong appIndex = new AtomicLong(0);

    private static Object globalLock = new Object();
    
    private volatile static FrameworkModel defaultInstance;

    private volatile ApplicationModel defaultAppModel;

    private static List<FrameworkModel> allInstances = new CopyOnWriteArrayList<>();

    private List<ApplicationModel> applicationModels = new CopyOnWriteArrayList<>();

    private List<ApplicationModel> pubApplicationModels = new CopyOnWriteArrayList<>();

    private FrameworkServiceRepository serviceRepository;

    private ApplicationModel internalApplicationModel;

    private Object instLock = new Object();

    public FrameworkModel() {
        super(null, ExtensionScope.FRAMEWORK);
        this.setInternalId(String.valueOf(index.getAndIncrement()));
        // register FrameworkModel instance early
        synchronized (globalLock) {
            allInstances.add(this);
            resetDefaultFrameworkModel();
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(getDesc() + " is created");
        }
        initialize();
    }

    @Override
    protected void initialize() {
        super.initialize();

        TypeDefinitionBuilder.initBuilders(this);

        serviceRepository = new FrameworkServiceRepository(this);

        ExtensionLoader<ScopeModelInitializer> initializerExtensionLoader = this.getExtensionLoader(ScopeModelInitializer.class);
        Set<ScopeModelInitializer> initializers = initializerExtensionLoader.getSupportedExtensionInstances();
        for (ScopeModelInitializer initializer : initializers) {
            initializer.initializeFrameworkModel(this);
        }

        internalApplicationModel = new ApplicationModel(this, true);
        internalApplicationModel.getApplicationConfigManager().setApplication(
            new ApplicationConfig(internalApplicationModel, CommonConstants.DUBBO_INTERNAL_APPLICATION));
        internalApplicationModel.setModelName(CommonConstants.DUBBO_INTERNAL_APPLICATION);
    }

    @Override
    protected void onDestroy() {
        if (defaultInstance == this) {
            // NOTE: During destroying the default FrameworkModel, the FrameworkModel.defaultModel() or ApplicationModel.defaultModel()
            // will return a broken model, maybe cause unpredictable problem.
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Destroying default framework model: " + getDesc());
            }
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(getDesc() + " is destroying ...");
        }

        // destroy all application model
        for (ApplicationModel applicationModel : new ArrayList<>(applicationModels)) {
            applicationModel.destroy();
        }
        // check whether all application models are destroyed
        checkApplicationDestroy();

        // notify destroy and clean framework resources
        // see org.apache.dubbo.config.deploy.FrameworkModelCleaner
        notifyDestroy();
        checkApplicationDestroy();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(getDesc() + " is destroyed");
        }

        // remove from allInstances and reset default FrameworkModel
        synchronized (globalLock) {
            allInstances.remove(this);
            resetDefaultFrameworkModel();
        }

        // if all FrameworkModels are destroyed, clean global static resources, shutdown dubbo completely
        destroyGlobalResources();
    }

    private void checkApplicationDestroy() {
        if (applicationModels.size() > 0) {
            List<String> remainApplications = applicationModels.stream()
                .map(model -> model.getDesc())
                .collect(Collectors.toList());
            throw new IllegalStateException("Not all application models are completely destroyed, remaining " +
                remainApplications.size() + " application models may be created during destruction: " + remainApplications);
        }
    }

    private void destroyGlobalResources() {
        synchronized (globalLock) {
            if (allInstances.isEmpty()) {
                GlobalResourcesRepository.getInstance().destroy();
            }
        }
    }

    /**
     * During destroying the default FrameworkModel, the FrameworkModel.defaultModel() or ApplicationModel.defaultModel()
     * will return a broken model, maybe cause unpredictable problem.
     * Recommendation: Avoid using the default model as much as possible.
     * @return the global default FrameworkModel
     */
    public static FrameworkModel defaultModel() {
        FrameworkModel instance = defaultInstance;
        if (instance == null) {
            synchronized (globalLock) {
                resetDefaultFrameworkModel();
                if (defaultInstance == null) {
                    defaultInstance = new FrameworkModel();
                }
                instance = defaultInstance;
            }
        }
        Assert.notNull(instance, "Default FrameworkModel is null");
        return instance;
    }

    /**
     * Get all framework model instances
     * @return
     */
    public static List<FrameworkModel> getAllInstances() {
        return Collections.unmodifiableList(new ArrayList<>(allInstances));
    }

    /**
     * Destroy all framework model instances, shutdown dubbo engine completely.
     */
    public static void destroyAll() {
        for (FrameworkModel frameworkModel : new ArrayList<>(allInstances)) {
            frameworkModel.destroy();
        }
    }

    public ApplicationModel newApplication() {
        return new ApplicationModel(this);
    }

    /**
     * Get or create default application model
     * @return
     */
    public ApplicationModel defaultApplication() {
        ApplicationModel appModel = this.defaultAppModel;
        if (appModel == null) {
            // check destroyed before acquire inst lock, avoid blocking during destroying
            checkDestroyed();
            resetDefaultAppModel();
            if ((appModel = this.defaultAppModel) == null) {
                synchronized (instLock) {
                    if (this.defaultAppModel == null) {
                        this.defaultAppModel = newApplication();
                    }
                    appModel = this.defaultAppModel;
                }
            }
        }
        Assert.notNull(appModel, "Default ApplicationModel is null");
        return appModel;
    }

    ApplicationModel getDefaultAppModel() {
        return defaultAppModel;
    }

    void addApplication(ApplicationModel applicationModel) {
        // can not add new application if it's destroying
        checkDestroyed();
        synchronized (instLock) {
            if (!this.applicationModels.contains(applicationModel)) {
                applicationModel.setInternalId(buildInternalId(getInternalId(), appIndex.getAndIncrement()));
                this.applicationModels.add(applicationModel);
                if (!applicationModel.isInternal()) {
                    this.pubApplicationModels.add(applicationModel);
                }
                resetDefaultAppModel();
            }
        }
    }

    void removeApplication(ApplicationModel model) {
        synchronized (instLock) {
            this.applicationModels.remove(model);
            if (!model.isInternal()) {
                this.pubApplicationModels.remove(model);
            }
            resetDefaultAppModel();
        }
    }

    void tryDestroy() {
        synchronized (instLock) {
            if (pubApplicationModels.size() == 0) {
                destroy();
            }
        }
    }

    private void checkDestroyed() {
        if (isDestroyed()) {
            throw new IllegalStateException("FrameworkModel is destroyed");
        }
    }

    private void resetDefaultAppModel() {
        synchronized (instLock) {
            if (this.defaultAppModel != null && !this.defaultAppModel.isDestroyed()) {
                return;
            }
            ApplicationModel oldDefaultAppModel = this.defaultAppModel;
            if (pubApplicationModels.size() > 0) {
                this.defaultAppModel = pubApplicationModels.get(0);
            } else {
                this.defaultAppModel = null;
            }
            if (defaultInstance == this && oldDefaultAppModel != this.defaultAppModel) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Reset global default application from " + safeGetModelDesc(oldDefaultAppModel) + " to " + safeGetModelDesc(this.defaultAppModel));
                }
            }
        }
    }

    private static void resetDefaultFrameworkModel() {
        synchronized (globalLock) {
            if (defaultInstance != null && !defaultInstance.isDestroyed()) {
                return;
            }
            FrameworkModel oldDefaultFrameworkModel = defaultInstance;
            if (allInstances.size() > 0) {
                defaultInstance = allInstances.get(0);
            } else {
                defaultInstance = null;
            }
            if (oldDefaultFrameworkModel != defaultInstance) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Reset global default framework from " + safeGetModelDesc(oldDefaultFrameworkModel) + " to " + safeGetModelDesc(defaultInstance));
                }
            }
        }
    }

    private static String safeGetModelDesc(ScopeModel scopeModel) {
        return scopeModel != null ? scopeModel.getDesc() : null;
    }

    public List<ApplicationModel> getApplicationModels() {
        return Collections.unmodifiableList(pubApplicationModels);
    }

    public List<ApplicationModel> getAllApplicationModels() {
        return Collections.unmodifiableList(applicationModels);
    }

    public ApplicationModel getInternalApplicationModel() {
        return internalApplicationModel;
    }

    public FrameworkServiceRepository getServiceRepository() {
        return serviceRepository;
    }

    @Override
    public Environment getModelEnvironment() {
        throw new UnsupportedOperationException("Environment is inaccessible for FrameworkModel");
    }

    @Override
    protected boolean checkIfClassLoaderCanRemoved(ClassLoader classLoader) {
        return super.checkIfClassLoaderCanRemoved(classLoader) &&
            applicationModels.stream().noneMatch(applicationModel -> applicationModel.containsClassLoader(classLoader));
    }
}
