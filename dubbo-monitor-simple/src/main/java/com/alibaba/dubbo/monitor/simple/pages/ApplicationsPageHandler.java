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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Menu;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.monitor.simple.RegistryContainer;

/**
 * ApplicationsPageHandler
 * 
 * @author william.liangf
 */
@Menu(name = "Applications", desc = "Show application dependencies.", order = 1000)
public class ApplicationsPageHandler implements PageHandler {

    public Page handle(URL url) {
        Set<String> applications = RegistryContainer.getInstance().getApplications();
        List<List<String>> rows = new ArrayList<List<String>>();
        int providersCount = 0;
        int consumersCount = 0;
        int efferentCount = 0;
        int afferentCount = 0;
        if (applications != null && applications.size() > 0) {
            for (String application : applications) {
                List<String> row = new ArrayList<String>();
                row.add(application);
                
                List<URL> providers = RegistryContainer.getInstance().getProvidersByApplication(application);
                List<URL> consumers = RegistryContainer.getInstance().getConsumersByApplication(application);

                if (providers != null && providers.size() > 0
                        || consumers != null && consumers.size() > 0) {
                    URL provider = (providers != null && providers.size() > 0 ? providers.iterator().next() : consumers.iterator().next());
                    row.add(provider.getParameter("owner", "") + (provider.hasParameter("organization") ?  " (" + provider.getParameter("organization") + ")" : ""));
                } else {
                    row.add("");
                }
                
                int providersSize = providers == null ? 0 : providers.size();
                providersCount += providersSize;
                row.add(providersSize == 0 ? "<font color=\"blue\">No provider</font>" : "<a href=\"providers.html?application=" + application + "\">Providers(" + providersSize + ")</a>");
                
                int consumersSize = consumers == null ? 0 : consumers.size();
                consumersCount += consumersSize;
                row.add(consumersSize == 0 ? "<font color=\"blue\">No consumer</font>" : "<a href=\"consumers.html?application=" + application + "\">Consumers(" + consumersSize + ")</a>");
                
                Set<String> efferents = RegistryContainer.getInstance().getDependencies(application, false);
                int efferentSize = efferents == null ? 0 : efferents.size();
                efferentCount += efferentSize;
                row.add(efferentSize == 0 ? "<font color=\"blue\">No dependency</font>" : "<a href=\"dependencies.html?application=" + application + "\">Depends On(" + efferentSize + ")</a>");
                
                Set<String> afferents = RegistryContainer.getInstance().getDependencies(application, true);
                int afferentSize = afferents == null ? 0 : afferents.size();
                afferentCount += afferentSize;
                row.add(afferentSize == 0 ? "<font color=\"blue\">No used</font>" : "<a href=\"dependencies.html?application=" + application + "&reverse=true\">Used By(" + afferentSize + ")</a>");
                rows.add(row);
            }
        }
        return new Page("Applications", "Applications (" + rows.size() + ")",
                new String[] { "Application Name:", "Owner", "Providers(" + providersCount + ")", "Consumers(" + consumersCount + ")", "Depends On(" + efferentCount + ")", "Used By(" + afferentCount + ")" }, rows);
    }
    
}