package org.apache.dubbo.rpc;

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

import org.apache.dubbo.common.extension.SPI;

/**
 * this interface is used to enable graceful shutdown
 * any class implements this interface should be a spring bean or add spi extension config
 * if you use spring ,ShutdownHookListener will use it and flush resources in ContextClosedEvent
 * Without spring,please add a file under META-INFO/dubbo/internal which named org.apache.dubbo.rpc.GracefulShutDown
 * and then write graceful=xxx.xxx.xxx.Xxx which implement GracefulShutDown into it
 * application will use DubboShutdownHook to invoke afterRegistriesDestroyed and afterProtocolDestroyed
 * to flush resources in closed stage
 * @since 2.7.3
 */
@SPI
public interface GracefulShutDown {

    /**
     * SPI name which should be write in META-INFO/dubbo/internal/org.apache.dubbo.rpc.GracefulShutDown without spring
     */
    public static final String MARK = "graceful";

    /**
     * it is the beginning stage of shut down
     * some resources should be closed after  dubbo registries are destroyed but before dubbo connections are destroyed
     * so you should do something in close method
     * at this moment,some dubbo rpc invoke may be executing now,so any destroy method for are forbidden
     * for example, kafka message consumer should stop the poll method at this stage
     */
    default void afterRegistriesDestroyed(){}

    /**
     * it is the last stage for shut down
     * some operations should be done after dubbo connections destroyed
     * for example,some cache should be flush to db,or some objects with spring lifecycle should be destroyed
     */
    default void afterProtocolDestroyed(){}
}
