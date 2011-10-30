/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.rmi;

import java.rmi.Remote;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.protocol.AbstractExporter;

/**
 * Rmi exporter.
 * 
 * @author qian.lei
 */
public class RmiExporter<T> extends AbstractExporter<T> {
    
    private static final Logger Log = LoggerFactory.getLogger(RmiExporter.class);

    private Remote              remote;

    private Registry            registry;

    public RmiExporter(Invoker<T> invoker, Remote remote, Registry registry) {
        super(invoker);
        this.remote = remote;
        this.registry = registry;
    }

    public void unexport() {
        super.unexport();
        // unexport.
        if (remote != null) {
            try {
                UnicastRemoteObject.unexportObject(remote, true);
            } catch (Exception e) {
                Log.warn("Unexport rmi object error.", e); //ignore it.
            }
            remote = null;
        }
        if (registry != null) {
            try {
                // unbind.
                registry.unbind(getInvoker().getUrl().getPath());
            } catch (Exception e) {
                Log.warn("Unexport rmi object error.", e); //ignore it.
            }
            registry = null;
        }
    }

}