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
package com.alibaba.dubbo.common.serialize.hessian2.dubbo;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.StringUtils;

public class Hessian2FactoryUtil {
    static String WHITELIST = "dubbo.application.hessian2.whitelist";
    static String ALLOW = "dubbo.application.hessian2.allow";
    static String DENY = "dubbo.application.hessian2.deny";
    static ExtensionLoader<Hessian2FactoryInitializer> loader = ExtensionLoader.getExtensionLoader(Hessian2FactoryInitializer.class);

    public static Hessian2FactoryInitializer getInstance() {
        String whitelist = ConfigUtils.getProperty(WHITELIST);
        if (StringUtils.isNotEmpty(whitelist)) {
            return loader.getExtension("whitelist");
        }
        return loader.getDefaultExtension();
    }
}
