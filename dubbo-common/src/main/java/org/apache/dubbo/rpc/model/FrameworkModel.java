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
import org.apache.dubbo.config.ApplicationConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Model of dubbo framework, it can be shared with multiple applications.
 */
public class FrameworkModel extends ScopeModel {

    protected static final Logger LOGGER = LoggerFactory.getLogger(FrameworkModel.class);

    public static final String NAME = "FrameworkModel";
    private static final AtomicLong index = new AtomicLong(1);
    // internal app index is 0, default app index is 1
    private final AtomicLong appIndex = new AtomicLong(0);

    private volatile static FrameworkModel defaultInstance;

    private volatile ApplicationModel defaultAppModel;

    private static List<FrameworkModel> allInstances = Collections.synchronizedList(new ArrayList<>());

    private List<ApplicationModel> applicationModels = Collections.synchronizedList(new ArrayList<>());

    private List<ApplicationModel> pubApplicationModels = Collections.synchronizedList(new ArrayList<>());

    private FrameworkServiceRepository serviceRepository;

    private ApplicationModel internalApplicationModel;

    private Object lock = new Object();

    public FrameworkModel() {
        super(null, ExtensionScope.FRAMEWORK);
        // register FrameworkModel instance early
        synchronized (FrameworkModel.class) {
            allInstances.add(this);
            resetDefaultFrameworkModel();
        }
        initialize();
        this.setInternalId(index.getAndIncrement()+"");
    }

    @Override
    protected void initialize() {
        super.initialize();
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
        synchronized (lock) {
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
        }

        synchronized (FrameworkModel.class) {
            allInstances.remove(this);
            resetDefaultFrameworkModel();
        }

        // if all FrameworkModels are destroyed, clean global static resources, shutdown dubbo completely
        destroyGlobalResources();
    }

    private void checkApplicationDestroy() {
        if (applicationModels.size() > 0) {
            List<String> remainApplications = new ArrayList<>();
            for (ApplicationModel applicationModel : applicationModels) {
                remainApplications.add(applicationModel.getDesc());
            }
            throw new IllegalStateException("Not all application models are completely destroyed, remaining " +
                remainApplications.size() + " application models may be created during destruction: " + remainApplications);
        }
    }

    private void destroyGlobalResources() {
        synchronized (FrameworkModel.class) {
            if (allInstances.isEmpty()) {
                GlobalResourcesRepository.getInstance().destroy();
            }
        }
    }

    public static FrameworkModel defaultModel() {
        if (defaultInstance == null) {
            synchronized (FrameworkModel.class) {
                if (defaultInstance == null) {
                    defaultInstance = new FrameworkModel();
                }
            }
        }
        return defaultInstance;
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
        if (defaultAppModel == null) {
            checkDestroyed();
            synchronized(lock){
                resetDefaultAppModel();
                if (defaultAppModel == null) {
                    defaultAppModel = newApplication();
                }
            }
        }
        return defaultAppModel;
    }

    ApplicationModel getDefaultAppModel() {
        return defaultAppModel;
    }

    void addApplication(ApplicationModel applicationModel) {
        // can not add new application if it's destroying
        checkDestroyed();
        synchronized (lock){
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
        synchronized (lock) {
            this.applicationModels.remove(model);
            if (!model.isInternal()) {
                this.pubApplicationModels.remove(model);
            }
            resetDefaultAppModel();
        }
    }

    void tryDestroy() {
        synchronized (lock) {
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
        synchronized (lock) {
            ApplicationModel oldDefaultAppModel = this.defaultAppModel;
            if (pubApplicationModels.size() > 0) {
                this.defaultAppModel = pubApplicationModels.get(0);
            } else {
                this.defaultAppModel = null;
            }
            if (defaultInstance == this && oldDefaultAppModel != this.defaultAppModel) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Reset global default application from " + getModelDesc(oldDefaultAppModel) + " to " + getModelDesc(this.defaultAppModel));
                }
            }
        }
    }

    private void resetDefaultFrameworkModel() {
        synchronized (FrameworkModel.class) {
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
                    LOGGER.info("Reset global default framework from " + getModelDesc(oldDefaultFrameworkModel) + " to " + getModelDesc(defaultInstance));
                }
            }
        }
    }

    private String getModelDesc(ScopeModel scopeModel) {
        return scopeModel != null ? "[" + scopeModel.getInternalId() + "]" : null;
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
