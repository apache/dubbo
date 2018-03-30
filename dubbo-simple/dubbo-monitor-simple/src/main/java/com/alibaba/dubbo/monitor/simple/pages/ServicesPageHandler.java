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
package com.alibaba.dubbo.monitor.simple.pages;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Menu;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.monitor.simple.RegistryContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ServicesPageHandler
 */
@Menu(name = "Services", desc = "Show registered services.", order = 2000)
public class ServicesPageHandler implements PageHandler {

    public Page handle(URL url) {
        Set<String> services = RegistryContainer.getInstance().getServices();
        List<List<String>> rows = new ArrayList<List<String>>();
        int providerCount = 0;
        int consumerCount = 0;
        if (services != null && services.size() > 0) {
            for (String service : services) {
                List<URL> providers = RegistryContainer.getInstance().getProvidersByService(service);
                int providerSize = providers == null ? 0 : providers.size();
                providerCount += providerSize;
                List<URL> consumers = RegistryContainer.getInstance().getConsumersByService(service);
                int consumerSize = consumers == null ? 0 : consumers.size();
                consumerCount += consumerSize;
                List<String> row = new ArrayList<String>();
                row.add(service);
                if (providerSize > 0 || consumerSize > 0) {
                    if (providerSize > 0) {
                        URL provider = providers.iterator().next();
                        row.add(provider.getParameter(Constants.APPLICATION_KEY, ""));
                        row.add(provider.getParameter("owner", "") + (provider.hasParameter("organization") ? " (" + provider.getParameter("organization") + ")" : ""));
                    } else {
                        row.add("");
                        row.add("");
                    }
                    row.add(providerSize == 0 ? "<font color=\"red\">No provider</a>" : "<a href=\"providers.html?service=" + service + "\">Providers(" + providerSize + ")</a>");
                    row.add(consumerSize == 0 ? "<font color=\"blue\">No consumer</a>" : "<a href=\"consumers.html?service=" + service + "\">Consumers(" + consumerSize + ")</a>");
                    row.add("<a href=\"statistics.html?service=" + service + "\">Statistics</a>");
                    row.add("<a href=\"charts.html?service=" + service + "\">Charts</a>");
                    rows.add(row);
                }
            }
        }
        return new Page("Services", "Services (" + rows.size() + ")",
                new String[]{"Service Name:", "Application", "Owner", "Providers(" + providerCount + ")", "Consumers(" + consumerCount + ")", "Statistics", "Charts"}, rows);
    }

}