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
package com.alibaba.dubbo.registry.admin.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.registry.admin.RegistryContainer;

/**
 * ConsumersPageHandler
 * 
 * @author william.liangf
 */
@Extension("consumers")
public class ConsumersPageHandler implements PageHandler {
    
    public Page handle(URL url) {
        String service = url.getParameter("service");
        List<List<String>> rows = new ArrayList<List<String>>();
        String nav;
        if (service != null && service.length() > 0) {
            List<URL> consumers = RegistryContainer.getInstance().getConsumers(service);
            if (consumers != null && consumers.size() > 0) {
                for (URL consumer : consumers) {
                    List<String> row = new ArrayList<String>();
                    row.add(consumer.toFullString());
                    rows.add(row);
                }
            }
            nav = "<a href=\"services.html\">Services</a> &gt; " + service 
                    + " &gt; <a href=\"providers.html?service=" + service 
                    + "\">Providers</a> | Consumers | <a href=\"routes.html?service=" + service + "\">Routes</a>";
        } else {
            Collection<List<URL>> values = RegistryContainer.getInstance().getConsumers().values();
            if (values != null && values.size() > 0) {
                for (List<URL> consumers : values) {
                    if (consumers != null && consumers.size() > 0) {
                        for (URL consumer : consumers) {
                            List<String> row = new ArrayList<String>();
                            row.add(consumer.toFullString());
                            rows.add(row);
                        }
                    }
                }
            }
            nav = "Consumers";
        }
        return new Page("<a href=\"/\">Home</a> &gt; " + nav, "Consumers (" + rows.size() + ")",
                new String[] { "Provider URL:" }, rows);
    }

}
