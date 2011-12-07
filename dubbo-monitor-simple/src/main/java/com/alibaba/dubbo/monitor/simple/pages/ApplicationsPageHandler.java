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

import com.alibaba.dubbo.common.Extension;
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
@Menu(name = "Applications", desc = "Applications", order = 2000)
@Extension("applications")
public class ApplicationsPageHandler implements PageHandler {

    public Page handle(URL url) {
        Set<String> applications = RegistryContainer.getInstance().getApplications();
        List<List<String>> rows = new ArrayList<List<String>>();
        int efferentCount = 0;
        int afferentCount = 0;
        if (applications != null && applications.size() > 0) {
            for (String application : applications) {
                List<String> row = new ArrayList<String>();
                row.add(application);
                Set<String> efferents = RegistryContainer.getInstance().getEfferentDependencies(application);
                int efferentSize = efferents == null ? 0 : efferents.size();
                efferentCount += efferentSize;
                row.add(efferentSize == 0 ? "<font color=\"blue\">No Efferent Dependencies</font>" : "<a href=\"dependencies.html?direction=efferent&application=" + application + "\">Efferent Dependencies(" + efferentSize + ")</a>");
                Set<String> afferents = RegistryContainer.getInstance().getAfferentDependencies(application);
                int afferentSize = afferents == null ? 0 : afferents.size();
                afferentCount += afferentSize;
                row.add(afferentSize == 0 ? "<font color=\"blue\">No Afferent Dependencies</font>" : "<a href=\"dependencies.html?direction=afferent&application=" + application + "\">Afferent Dependencies(" + afferentSize + ")</a>");
                rows.add(row);
            }
        }
        return new Page("Apploications", "Apploications (" + rows.size() + ")",
                new String[] { "Application Name:", "Efferent Dependencies(" + efferentCount + ")", "Afferent Dependencies(" + afferentCount + ")" }, rows);
    }
    
}
