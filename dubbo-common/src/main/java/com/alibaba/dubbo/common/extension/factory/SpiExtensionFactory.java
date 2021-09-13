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
package com.alibaba.dubbo.common.extension.factory;

import com.alibaba.dubbo.common.extension.ExtensionFactory;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * SpiExtensionFactory
 */
/**
 * @date 2021/9/13
 * @author huangchenguang
 * @desc Spi扩展类工厂
 */
public class SpiExtensionFactory implements ExtensionFactory {

    /**
     * @date 2021/9/13
     * @author huangchenguang
     * @desc 生产Spi扩展类工厂对应的对象
     */
    @Override
    public <T> T getExtension(Class<T> type, String name) {
        // 判断是否为接口，接口上是否有@SPI注解
        if (type.isInterface() && type.isAnnotationPresent(SPI.class)) {
            // 获取扩展类加载器
            ExtensionLoader<T> loader = ExtensionLoader.getExtensionLoader(type);
            if (!loader.getSupportedExtensions().isEmpty()) {
                // 返回扩展适配器对象
                return loader.getAdaptiveExtension();
            }
        }
        return null;
    }

}
