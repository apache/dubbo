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
package org.apache.dubbo.common.extension.director.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.director.FooAppProvider;
import org.apache.dubbo.common.extension.director.FooFrameworkProvider;
import org.apache.dubbo.common.extension.director.FooFrameworkService;
import org.apache.dubbo.common.extension.director.FooModuleProvider;

public class TestFrameworkService extends BaseTestService implements FooFrameworkService {

    private FooFrameworkProvider frameworkProvider;

    private FooAppProvider appProvider;

    private FooModuleProvider moduleProvider;

    public FooFrameworkProvider getFrameworkProvider() {
        return frameworkProvider;
    }

    public void setFrameworkProvider(FooFrameworkProvider frameworkProvider) {
        this.frameworkProvider = frameworkProvider;
    }

    public FooAppProvider getAppProvider() {
        return appProvider;
    }

    public void setAppProvider(FooAppProvider appProvider) {
        this.appProvider = appProvider;
    }

    public FooModuleProvider getModuleProvider() {
        return moduleProvider;
    }

    public void setModuleProvider(FooModuleProvider moduleProvider) {
        this.moduleProvider = moduleProvider;
    }

    @Override
    public void process(URL url) {

    }
}
