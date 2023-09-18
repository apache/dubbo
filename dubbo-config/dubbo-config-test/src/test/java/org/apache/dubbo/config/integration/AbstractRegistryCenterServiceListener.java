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
package org.apache.dubbo.config.integration;

import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ServiceListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This implementation of {@link ServiceListener} is to record exported services, which should be extended by different sub-classes.
 */
public abstract class AbstractRegistryCenterServiceListener implements ServiceListener {

    private List<ServiceConfig> exportedServices = new ArrayList<>(2);

    /**
     * Return the interface name of exported service.
     */
    protected abstract Class<?> getInterface();
    /**
     * {@inheritDoc}
     */
    @Override
    public void exported(ServiceConfig sc) {
        //All exported services will be added
        if (sc.getInterfaceClass() == getInterface()) {
            exportedServices.add(sc);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unexported(ServiceConfig sc) {
        //remove the exported services.
        exportedServices.remove(sc);
    }

    /**
     * Return all exported services.
     */
    public List<ServiceConfig> getExportedServices() {
        return Collections.unmodifiableList(exportedServices);
    }
}
