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
package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;

/**
 * RegistryProtocol listener is introduced to provide a chance to user to customize or change export and refer behavior
 * of RegistryProtocol. For example: re-export or re-refer on the fly when certain condition meets.
 */
@SPI
public interface RegistryProtocolListener {
    /**
     * Notify RegistryProtocol's listeners when a service is registered
     *
     * @param registryProtocol RegistryProtocol instance
     * @param exporter         exporter
     * @see RegistryProtocol#export(org.apache.dubbo.rpc.Invoker)
     */
    void onExport(RegistryProtocol registryProtocol, Exporter<?> exporter);

    /**
     * Notify RegistryProtocol's listeners when a service is subscribed
     *
     * @param registryProtocol RegistryProtocol instance
     * @param invoker          invoker
     * @see RegistryProtocol#refer(Class, URL)
     */
    void onRefer(RegistryProtocol registryProtocol, Invoker<?> invoker);

    /**
     * Notify RegistryProtocol's listeners when the protocol is destroyed
     */
    void onDestroy();
}
