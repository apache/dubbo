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
package com.alibaba.dubbo.rpc.dubbo.status;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.support.header.HeaderExchangeServer;
import com.alibaba.dubbo.remoting.transport.support.handler.WrappedChannelHandler;
import com.alibaba.dubbo.rpc.dubbo.DubboProtocol;

/**
 * ThreadPoolStatusChecker
 * 
 * @author william.liangf
 */
@Extension("threadpool")
public class ThreadPoolStatusChecker implements StatusChecker {

    public Status check() {
        Collection<ExchangeServer> servers = DubboProtocol.getDubboProtocol().getServers();
        if (servers == null || servers.size() == 0) {
            return new Status(Status.Level.UNKNOWN);
        }
        for (Server server : servers) {
            if (server instanceof HeaderExchangeServer) {
                HeaderExchangeServer exchanger = (HeaderExchangeServer) server;
                server = exchanger.getServer();
            }
            ChannelHandler handler = server.getChannelHandler();
            if (handler instanceof WrappedChannelHandler) {
                Executor executor = ((WrappedChannelHandler) handler).getExecutor();
                if (executor instanceof ThreadPoolExecutor) {
                    ThreadPoolExecutor tp = (ThreadPoolExecutor)executor;
                    boolean ok = tp.getActiveCount() < tp.getMaximumPoolSize() - 1;
                    return new Status(ok ? Status.Level.OK : Status.Level.WARN, 
                            "max:" + tp.getMaximumPoolSize() 
                            + ",core:" + tp.getCorePoolSize() 
                            + ",largest:" + tp.getLargestPoolSize()
                            + ",active:" + tp.getActiveCount() 
                            + ",task:" + tp.getTaskCount());
                }
            }
        }
        return new Status(Status.Level.UNKNOWN);
    }

}