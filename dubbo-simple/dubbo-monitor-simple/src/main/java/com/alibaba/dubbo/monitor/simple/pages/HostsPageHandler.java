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
package com.alibaba.dubbo.monitor.simple.pages;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.container.page.Menu;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.monitor.simple.RegistryContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * HostsPageHandler
 *
 * @author william.liangf
 */
@Menu(name = "Hosts", desc = "Show provider and consumer hosts", order = 3000)
public class HostsPageHandler implements PageHandler {

    public Page handle(URL url) {
        List<List<String>> rows = new ArrayList<List<String>>();
        Set<String> hosts = RegistryContainer.getInstance().getHosts();
        int providersCount = 0;
        int consumersCount = 0;
        if (hosts != null && hosts.size() > 0) {
            for (String host : hosts) {
                List<String> row = new ArrayList<String>();
                row.add(NetUtils.getHostName(host) + "/" + host);

                List<URL> providers = RegistryContainer.getInstance().getProvidersByHost(host);
                List<URL> consumers = RegistryContainer.getInstance().getConsumersByHost(host);

                if (providers != null && providers.size() > 0
                        || consumers != null && consumers.size() > 0) {
                    URL provider = (providers != null && providers.size() > 0 ? providers.iterator().next() : consumers.iterator().next());
                    row.add(provider.getParameter(Constants.APPLICATION_KEY, ""));
                    row.add(provider.getParameter("owner", "") + (provider.hasParameter("organization") ? " (" + provider.getParameter("organization") + ")" : ""));
                } else {
                    row.add("");
                    row.add("");
                }

                int proviedSize = providers == null ? 0 : providers.size();
                providersCount += proviedSize;
                row.add(proviedSize == 0 ? "<font color=\"blue\">No provider</font>" : "<a href=\"providers.html?host=" + host + "\">Providers(" + proviedSize + ")</a>");

                int consumersSize = consumers == null ? 0 : consumers.size();
                consumersCount += consumersSize;
                row.add(consumersSize == 0 ? "<font color=\"blue\">No consumer</font>" : "<a href=\"consumers.html?host=" + host + "\">Consumers(" + consumersSize + ")</a>");

                rows.add(row);
            }
        }
        return new Page("Hosts", "Hosts (" + rows.size() + ")",
                new String[]{"Host Name/IP:", "Application", "Owner", "Providers(" + providersCount + ")", "Consumers(" + consumersCount + ")"}, rows);
    }

}