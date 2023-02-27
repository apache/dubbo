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
package org.apache.dubbo.config.deploy;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModelDestroyListener;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_UNDEFINED_PROTOCOL;

/**
 * A cleaner to release resources of framework model
 */
public class FrameworkModelCleaner implements ScopeModelDestroyListener<FrameworkModel> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(FrameworkModelCleaner.class);

    private final AtomicBoolean protocolDestroyed = new AtomicBoolean(false);

    @Override
    public boolean isProtocol() {
        return true;
    }

    @Override
    public void onDestroy(FrameworkModel frameworkModel) {
        destroyFrameworkResources(frameworkModel);
    }

    /**
     * Destroy all framework resources.
     */
    private void destroyFrameworkResources(FrameworkModel frameworkModel) {
        // destroy protocol in framework scope
        destroyProtocols(frameworkModel);
    }

    /**
     * Destroy all the protocols.
     */
    private void destroyProtocols(FrameworkModel frameworkModel) {
        if (protocolDestroyed.compareAndSet(false, true)) {
            ExtensionLoader<Protocol> loader = frameworkModel.getExtensionLoader(Protocol.class);
            for (String protocolName : loader.getLoadedExtensions()) {
                try {
                    Protocol protocol = loader.getLoadedExtension(protocolName);
                    if (protocol != null) {
                        protocol.destroy();
                    }
                } catch (Throwable t) {
                    logger.warn(CONFIG_UNDEFINED_PROTOCOL, "", "", t.getMessage(), t);
                }
            }
        }
    }

}
