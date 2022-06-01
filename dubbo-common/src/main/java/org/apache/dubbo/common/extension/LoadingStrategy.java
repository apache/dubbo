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
package org.apache.dubbo.common.extension;

import org.apache.dubbo.common.lang.Prioritized;

public interface LoadingStrategy extends Prioritized {

    String directory();

    default boolean preferExtensionClassLoader() {
        return false;
    }

    default String[] excludedPackages() {
        return null;
    }

    /**
     * To restrict some class that should not be loaded from `org.apache.dubbo` package type SPI class.
     * For example, we can restrict the implementation class which package is `org.xxx.xxx`
     * can be loaded as SPI implementation.
     *
     * @return packages can be loaded in `org.apache.dubbo`'s SPI
     */
    default String[] includedPackages() {
        // default match all
        return null;
    }

    /**
     * To restrict some class that should not be loaded from `org.alibaba.dubbo`(for compatible purpose)
     * package type SPI class.
     * For example, we can restrict the implementation class which package is `org.xxx.xxx`
     * can be loaded as SPI implementation
     *
     * @return packages can be loaded in `org.alibaba.dubbo`'s SPI
     */
    default String[] includedPackagesInCompatibleType() {
        // default match all
        return null;
    }

    /**
     * To restrict some class that should load from Dubbo's ClassLoader.
     * For example, we can restrict the class declaration in `org.apache.dubbo` package should
     * be loaded from Dubbo's ClassLoader and users cannot declare these classes.
     *
     * @return class packages should load
     * @since 3.0.4
     */
    default String[] onlyExtensionClassLoaderPackages() {
        return new String[]{};
    }

    /**
     * Indicates current {@link LoadingStrategy} supports overriding other lower prioritized instances or not.
     *
     * @return if supports, return <code>true</code>, or <code>false</code>
     * @since 2.7.7
     */
    default boolean overridden() {
        return false;
    }

    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * when spi is loaded by dubbo framework classloader only, it indicates all LoadingStrategy should load this spi
     */
    String ALL = "ALL";
}
