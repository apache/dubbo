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
package org.apache.dubbo.common.serialize.fastjson2;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;

import java.util.Arrays;

public class Fastjson2ScopeModelInitializer implements ScopeModelInitializer {

    @Override
    public void initializeFrameworkModel(FrameworkModel frameworkModel) {
        boolean classExist = false;
        try {
            for (String className : Arrays.asList(
                    "com.alibaba.fastjson2.JSONB",
                    "com.alibaba.fastjson2.reader.ObjectReaderCreatorASM",
                    "com.alibaba.fastjson2.writer.ObjectWriterCreatorASM",
                    "com.alibaba.fastjson2.JSONValidator",
                    "com.alibaba.fastjson2.JSONFactory",
                    "com.alibaba.fastjson2.JSONWriter",
                    "com.alibaba.fastjson2.util.TypeUtils",
                    "com.alibaba.fastjson2.filter.ContextAutoTypeBeforeHandler")) {
                Class<?> aClass =
                        ClassUtils.forName(className, Thread.currentThread().getContextClassLoader());
                if (aClass == null) {
                    throw new ClassNotFoundException(className);
                }
            }
            classExist = true;
        } catch (Throwable ignored) {
        }

        if (classExist) {
            ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();
            beanFactory.registerBean(Fastjson2CreatorManager.class);
            beanFactory.registerBean(Fastjson2SecurityManager.class);
        }
    }

    @Override
    public void initializeApplicationModel(ApplicationModel applicationModel) {}

    @Override
    public void initializeModuleModel(ModuleModel moduleModel) {}
}
