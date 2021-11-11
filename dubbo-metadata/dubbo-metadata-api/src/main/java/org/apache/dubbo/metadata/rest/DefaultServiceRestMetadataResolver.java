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
package org.apache.dubbo.metadata.rest;

import java.lang.reflect.Method;
import java.util.Set;


/**
 * The default implementation {@link ServiceRestMetadataResolver}
 *
 * @since 2.7.6
 */
public class DefaultServiceRestMetadataResolver extends AbstractServiceRestMetadataResolver {

    @Override
    protected boolean supports0(Class<?> serviceType) {
        return false;
    }

    @Override
    protected boolean isRestCapableMethod(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {
        return false;
    }

    @Override
    protected String resolveRequestMethod(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {
        return null;
    }

    @Override
    protected String resolveRequestPath(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass) {
        return null;
    }

    @Override
    protected void processProduces(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass, Set<String> produces) {

    }

    @Override
    protected void processConsumes(Method serviceMethod, Class<?> serviceType, Class<?> serviceInterfaceClass, Set<String> consumes) {

    }
}
