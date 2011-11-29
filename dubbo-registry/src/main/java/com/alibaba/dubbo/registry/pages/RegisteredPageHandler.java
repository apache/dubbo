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

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Menu;
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
@Menu(name = "Registered", desc = "Registered", order = 11000)
@Extension("registered")
public class RegisteredPageHandler implements PageHandler {

    public Page handle(URL url) {
        List<List<String>> rows = new ArrayList<List<String>>();
        Collection<Registry> registries = AbstractRegistryFactory.getRegistries();
        if (registries != null && registries.size() > 0) {
        	Registry registry = registries.iterator().next();
        	if (registry instanceof AbstractRegistry) {
            	Set<String> services = ((AbstractRegistry) registry).getRegistered();
                if (services != null && services.size() > 0) {
                    for (String u : services) {
                        List<String> row = new ArrayList<String>();
                        row.add(URL.valueOf(u).getServiceName().replace("<", "&lt;").replace(">", "&gt;"));
                        row.add(u.toString().replace("<", "&lt;").replace(">", "&gt;"));
                        rows.add(row);
                    }
                }
        	}
        }
        return new Page("<a href=\"/\">Home</a> &gt; Service", "Services (" + rows.size() + ")",
                new String[] { "Service Type:", "URL:" }, rows);
    }

}