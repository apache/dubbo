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

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.extension.ExtensionAccessor;
import org.apache.dubbo.common.extension.ExtensionDirector;

import java.util.Set;

public abstract class ScopeModel implements ExtensionAccessor {

    private final ScopeModel parent;

    private final ExtensionDirector extensionDirector;

    private final ScopeBeanFactory beanFactory;

    public ScopeModel(ScopeModel parent, ExtensionDirector extensionDirector) {
        this.parent = parent;
        this.extensionDirector = extensionDirector;
        this.extensionDirector.addExtensionPostProcessor(new ScopeModelAwareExtensionProcessor(this));
        this.beanFactory = new ScopeBeanFactory(parent != null ? parent.getBeanFactory() : null, extensionDirector);
    }

    public ExtensionDirector getExtensionDirector() {
        return extensionDirector;
    }

    public ScopeBeanFactory getBeanFactory() {
        return beanFactory;
    }

    public ScopeModel getParent() {
        return parent;
    }

    public void destroy() {
    }

    protected void postProcessAfterCreated() {
        Set<ScopeModelPostProcessor> scopeModelPostProcessors = getExtensionLoader(ScopeModelPostProcessor.class)
            .getSupportedExtensionInstances();
        for (ScopeModelPostProcessor processor : scopeModelPostProcessors) {
            processor.postProcessScopeModel(this);
        }
    }
}
