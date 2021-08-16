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
package org.apache.dubbo.serialize.hessian.dubbo;

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.utils.StringUtils;

import com.caucho.hessian.io.SerializerFactory;

@SPI("default")
public interface Hessian2FactoryInitializer {
    String WHITELIST = "dubbo.application.hessian2.whitelist";
    String ALLOW = "dubbo.application.hessian2.allow";
    String DENY = "dubbo.application.hessian2.deny";
    ExtensionLoader<Hessian2FactoryInitializer> loader = ExtensionLoader.getExtensionLoader(Hessian2FactoryInitializer.class);

    SerializerFactory getSerializerFactory();

    static Hessian2FactoryInitializer getInstance() {
        String whitelist = ConfigurationUtils.getProperty(WHITELIST);
        if (StringUtils.isNotEmpty(whitelist)) {
            return loader.getExtension("whitelist");
        }
        return loader.getDefaultExtension();
    }

}
