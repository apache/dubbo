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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.resource.GlobalResourcesRepository;

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
    // app index starts from 1 in each FrameworkModel
    private final AtomicLong appIndex = new AtomicLong(1);

    private volatile static FrameworkModel defaultInstance;

    private static List<FrameworkModel> allInstances = Collections.synchronizedList(new ArrayList<>());

    private List<ApplicationModel> applicationModels = Collections.synchronizedList(new ArrayList<>());

    private FrameworkServiceRepository serviceRepository;


    public FrameworkModel() {
        super(null, ExtensionScope.FRAMEWORK);
        initialize();
        this.setInternalName(buildInternalName(NAME, null, index.getAndIncrement()));
    }

    @Override
    protected void initialize() {
        super.initialize();
        serviceRepository = new FrameworkServiceRepository(this);
        allInstances.add(this);

        ExtensionLoader<ScopeModelInitializer> initializerExtensionLoader = this.getExtensionLoader(ScopeModelInitializer.class);
        Set<ScopeModelInitializer> initializers = initializerExtensionLoader.getSupportedExtensionInstances();
        for (ScopeModelInitializer initializer : initializers) {
            initializer.initializeFrameworkModel(this);
        }
    }

    @Override
    protected void onDestroy() {
        // destroy all application model
        for (ApplicationModel applicationModel : new ArrayList<>(applicationModels)) {
            applicationModel.destroy();
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Dubbo framework[" + getInternalId() + "] is destroying ...");
        }
        synchronized (FrameworkModel.class) {
            allInstances.remove(this);
            if (defaultInstance == this) {
                defaultInstance = null;
            }
        }

        // notify destroy and clean framework resources
        // see org.apache.dubbo.config.deploy.FrameworkModelCleaner
        notifyDestroy();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Dubbo framework[" + getInternalId() + "] is destroyed");
        }

        // if all FrameworkModels are destroyed, clean global static resources, shutdown dubbo completely
        if (allInstances.isEmpty()) {
            destroyGlobalResources();
        }
    }

    private void destroyGlobalResources() {
        GlobalResourcesRepository.getInstance().destroy();
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

    public static List<FrameworkModel> getAllInstances() {
        return Collections.unmodifiableList(allInstances);
    }

    public static void destroyAll() {
        for (FrameworkModel frameworkModel : new ArrayList<>(allInstances)) {
            frameworkModel.destroy();
        }
    }

    public ApplicationModel newApplication() {
        return new ApplicationModel(this);
    }

    synchronized void addApplication(ApplicationModel applicationModel) {
        if (!this.applicationModels.contains(applicationModel)) {
            this.applicationModels.add(applicationModel);
            applicationModel.setInternalName(buildInternalName(ApplicationModel.NAME, getInternalId(), appIndex.getAndIncrement()));
        }
    }

    synchronized void removeApplication(ApplicationModel model) {
        this.applicationModels.remove(model);
    }

    synchronized void tryDestroy() {
        if (applicationModels.size() == 0) {
            destroy();
        }
    }

    public List<ApplicationModel> getApplicationModels() {
        return Collections.unmodifiableList(applicationModels);
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
