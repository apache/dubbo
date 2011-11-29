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

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.web.Page;
import com.alibaba.dubbo.container.web.PageHandler;
import com.alibaba.dubbo.monitor.simple.RegistryContainer;

/**
 * ProvidersPageHandler
 * 
 * @author william.liangf
 */
@Extension("providers")
public class ProvidersPageHandler implements PageHandler {
    
    public Page handle(URL url) {
        String service = url.getParameter("service");
        if (service == null || service.length() == 0) {
            throw new IllegalArgumentException("Please input service parameter.");
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        List<URL> providers = RegistryContainer.getInstance().getProviders(service);
        if (providers != null && providers.size() > 0) {
            for (URL provider : providers) {
                List<String> row = new ArrayList<String>();
                row.add(provider.toFullString().replace("&", "&amp;"));
                row.add("<a href=\"statistics.html?service=" + service + "&provider=" + provider.getHost() + "\">Statistics</a>");
                rows.add(row);
            }
        }
        return new Page("<a href=\"services.html\">Services</a> &gt; " + service 
                + " &gt; Providers | <a href=\"consumers.html?service=" + service 
                + "\">Consumers</a> | <a href=\"statistics.html?service=" + service 
                + "\">Statistics</a>", "Providers (" + rows.size() + ")",
                new String[] { "Provider URL:", "Statistics" }, rows);
    }

}
