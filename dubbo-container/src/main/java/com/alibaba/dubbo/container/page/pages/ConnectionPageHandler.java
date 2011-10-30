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
package com.alibaba.dubbo.container.page.pages;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;

/**
 * ConnectionPageHandler
 * 
 * @author william.liangf
 */
@Extension("connection")
public class ConnectionPageHandler implements PageHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public Page handle(URL url) {
        List<List<String>> rows = new ArrayList<List<String>>();
        /*TBServer tbServer = TBServerManager.getInstance().getServer();
        Server server = tbServer.getServer();
        try {
            Field field = server.getClass().getDeclaredField("acceptor");
            field.setAccessible(true);
            IoAcceptor acceptor = (IoAcceptor) field.get(server);
            for (SocketAddress address : acceptor.getManagedServiceAddresses()) {
                for (IoSession session : acceptor.getManagedSessions(address)) {
                    List<String> row = new ArrayList<String>();
                    row.add(session.getRemoteAddress().toString().replace("<", "&lt;").replace(">",
                            "&gt;"));
                    rows.add(row);
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }*/
        return new Page("<a href=\"/\">Home</a> &gt; Connection", "Connections (" + rows.size() + ")",
                new String[] { "Client Address:" }, rows);
    }

}
