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
package org.apache.dubbo.common.lang;

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

/**
 * A callback interface invoked when Dubbo application is stopped.
 * <p>Note: This class is not directly related to Java ShutdownHook.</p>
 * <p/>
 * <p>Call chains:</p>
 * <ol>
 *     <li>Java Shutdown Hook -> ApplicationDeployer.destroy() -> execute ShutdownHookCallback</li>
 *     <li>Stop dubbo application -> ApplicationDeployer.destroy() -> execute ShutdownHookCallback</li>
 * </ol>
 *
 * @since 2.7.5
 * @see org.apache.dubbo.common.deploy.ApplicationDeployListener
 * @see org.apache.dubbo.rpc.model.ScopeModelDestroyListener
 */
@SPI(scope = ExtensionScope.APPLICATION)
public interface ShutdownHookCallback extends Prioritized {

    /**
     * Callback execution
     *
     * @throws Throwable if met with some errors
     */
    void callback() throws Throwable;
}
