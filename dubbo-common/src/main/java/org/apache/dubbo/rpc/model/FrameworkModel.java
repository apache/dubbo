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

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model of dubbo framework, it can be shared with multiple applications.
 */
public class FrameworkModel extends ScopeModel {

    protected static final Logger LOGGER = LoggerFactory.getLogger(FrameworkModel.class);

    private volatile static FrameworkModel defaultInstance;

    private static List<FrameworkModel> allInstances = Collections.synchronizedList(new ArrayList<>());

    private List<ApplicationModel> applicationModels = Collections.synchronizedList(new ArrayList<>());

    private FrameworkServiceRepository serviceRepository;


    public FrameworkModel() {
        super(null, ExtensionScope.FRAMEWORK);
        initialize();
    }

    @Override
    protected void initialize() {
        super.initialize();
        serviceRepository = new FrameworkServiceRepository(this);
        allInstances.add(this);

        postProcessAfterCreated();
    }

    @Override
    public void destroy() {
        //TODO destroy framework model
        for (ApplicationModel applicationModel : new ArrayList<>(applicationModels)) {
            applicationModel.destroy();
        }

        allInstances.remove(this);
        if (defaultInstance == this) {
            synchronized (FrameworkModel.class) {
                defaultInstance = null;
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

    synchronized void addApplication(ApplicationModel model) {
        if (!this.applicationModels.contains(model)) {
            this.applicationModels.add(model);
        }
    }

    synchronized void removeApplication(ApplicationModel model) {
        this.applicationModels.remove(model);
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

}
