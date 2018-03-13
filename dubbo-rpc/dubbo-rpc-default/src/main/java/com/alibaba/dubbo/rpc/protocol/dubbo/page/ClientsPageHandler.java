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
package com.alibaba.dubbo.rpc.protocol.dubbo.page;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ClientsPageHandler
 */
public class ClientsPageHandler implements PageHandler {

    public Page handle(URL url) {
        String port = url.getParameter("port");
        int p = port == null || port.length() == 0 ? 0 : Integer.parseInt(port);
        Collection<ExchangeServer> servers = DubboProtocol.getDubboProtocol().getServers();
        ExchangeServer server = null;
        StringBuilder select = new StringBuilder();
        if (servers != null && servers.size() > 0) {
            if (servers.size() == 1) {
                server = servers.iterator().next();
                String address = server.getUrl().getAddress();
                select.append(" &gt; " + NetUtils.getHostName(address) + "/" + address);
            } else {
                select.append(" &gt; <select onchange=\"window.location.href='clients.html?port=' + this.value;\">");
                for (ExchangeServer s : servers) {
                    int sp = s.getUrl().getPort();
                    select.append("<option value=\">");
                    select.append(sp);
                    if (p == 0 && server == null || p == sp) {
                        server = s;
                        select.append("\" selected=\"selected");
                    }
                    select.append("\">");
                    select.append(s.getUrl().getAddress());
                    select.append("</option>");
                }
                select.append("</select>");
            }
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        if (server != null) {
            Collection<ExchangeChannel> channels = server.getExchangeChannels();
            for (ExchangeChannel c : channels) {
                List<String> row = new ArrayList<String>();
                String address = NetUtils.toAddressString(c.getRemoteAddress());
                row.add(NetUtils.getHostName(address) + "/" + address);
                rows.add(row);
            }
        }
        return new Page("<a href=\"servers.html\">Servers</a>" + select.toString() + " &gt; Clients", "Clients (" + rows.size() + ")", new String[]{"Client Address:"}, rows);
    }

}