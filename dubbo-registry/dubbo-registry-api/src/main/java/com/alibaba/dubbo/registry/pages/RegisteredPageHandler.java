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
package com.alibaba.dubbo.registry.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.support.AbstractRegistry;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;

/**
 * RegisteredPageHandler
 * 
 * @author william.liangf
 */
public class RegisteredPageHandler implements PageHandler {

    public Page handle(URL url) {
        String registryAddress = url.getParameter("registry", "");
        List<List<String>> rows = new ArrayList<List<String>>();
        Collection<Registry> registries = AbstractRegistryFactory.getRegistries();
        StringBuilder select = new StringBuilder();
        Registry registry = null;
        if (registries != null && registries.size() > 0) {
            if (registries.size() == 1) {
                registry = registries.iterator().next();
                select.append(" &gt; " + registry.getUrl().getAddress());
            } else {
                select.append(" &gt; <select onchange=\"window.location.href='registered.html?registry=' + this.value;\">");
                for (Registry r : registries) {
                    String sp = r.getUrl().getAddress();
                    select.append("<option value=\">");
                    select.append(sp);
                    if (((registryAddress == null || registryAddress.length() == 0) && registry == null)
                            || registryAddress.equals(sp)) {
                        registry = r;
                        select.append("\" selected=\"selected");
                    }
                    select.append("\">");
                    select.append(sp);
                    select.append("</option>");
                }
                select.append("</select>");
            }
        }
        if (registry instanceof AbstractRegistry) {
            Set<URL> services = ((AbstractRegistry) registry).getRegistered();
            if (services != null && services.size() > 0) {
                for (URL u : services) {
                    List<String> row = new ArrayList<String>();
                    row.add(u.toFullString().replace("<", "&lt;").replace(">", "&gt;"));
                    rows.add(row);
                }
            }
        }
        return new Page("<a href=\"registries.html\">Registries</a>" + select.toString() + " &gt; Registered | <a href=\"subscribed.html?registry=" + registryAddress + "\">Subscribed</a>", "Registered (" + rows.size() + ")",
                new String[] { "Provider URL:" }, rows);
    }

}