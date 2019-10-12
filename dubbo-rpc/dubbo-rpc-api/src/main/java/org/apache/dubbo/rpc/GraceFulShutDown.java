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
/**
 * this interface is used to enable grace shutdown
 * any class implements this interface should be a spring bean or add spi extension config
 * @since 2.7.3
 */
public interface GraceFulShutDown {


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
