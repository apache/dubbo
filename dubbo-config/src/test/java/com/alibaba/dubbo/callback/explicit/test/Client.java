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
package com.alibaba.dubbo.callback.explicit.test;

import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;

public class Client {
    private static ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private static Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        URL consumerUrl = Server.serviceURL
//            .removeParameter("yyy."+Constants.ASYNC_KEY)
            ;
        
        //refer client 
        Invoker<IDemoService> reference = protocol.refer(IDemoService.class, consumerUrl);
        IDemoService demoProxy = (IDemoService)proxyFactory.getProxy(reference);
        
        //registry callback 
        demoProxy.xxx(new IDemoCallback() {
            public String yyy(String msg) {
                System.out.println("Recived callback: " + msg);
                return "ok";
            }
        },"other custom args");
        
        System.out.println("Async...");
        
        synchronized (Client.class) {
            Client.class.wait();
        }
    }
}