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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.dubbo.DubboCodec;
import com.alibaba.dubbo.rpc.proxy.ProxyFactory;

public class Server {
    private static ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private static Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    
    public final static URL serviceURL =  URL.valueOf("dubbo://127.0.0.1:20880/"+IDemoService.class.getName()+"?group=test" 
                +"&"+Constants.CODEC_KEY+"="+DubboCodec.NAME
                +"&"+Constants.DOWNSTREAM_CODEC_KEY+"="+DubboCodec.NAME
                +"&xxx.0.callback=true"
                +"&timeout="+Integer.MAX_VALUE
//                uncomment is unblock invoking
                +"&yyy."+Constants.ASYNC_KEY+"=true"
                );
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        //export 
        Invoker<IDemoService> invoker = proxyFactory.getInvoker(new DemoServiceImpl(), IDemoService.class, serviceURL);
        protocol.export(invoker);
        
        synchronized (Server.class) {
            Server.class.wait();
        }
    }
}