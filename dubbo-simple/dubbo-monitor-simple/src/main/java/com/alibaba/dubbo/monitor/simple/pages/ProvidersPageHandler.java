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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.monitor.simple.RegistryContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * ProvidersPageHandler
 */
public class ProvidersPageHandler implements PageHandler {

    public Page handle(URL url) {
        String service = url.getParameter("service");
        String host = url.getParameter("host");
        String application = url.getParameter("application");
        if (service != null && service.length() > 0) {
            List<List<String>> rows = new ArrayList<List<String>>();
            List<URL> providers = RegistryContainer.getInstance().getProvidersByService(service);
            if (providers != null && providers.size() > 0) {
                for (URL u : providers) {
                    List<String> row = new ArrayList<String>();
                    String s = u.toFullString();
                    row.add(s.replace("&", "&amp;"));
                    row.add("<button onclick=\"if(confirm('Confirm unregister provider?')){window.location.href='unregister.html?service=" + service + "&provider=" + URL.encode(s) + "';}\">Unregister</button>");
                    rows.add(row);
                }
            }
            return new Page("<a href=\"services.html\">Services</a> &gt; " + service
                    + " &gt; Providers | <a href=\"consumers.html?service=" + service
                    + "\">Consumers</a> | <a href=\"statistics.html?service=" + service
                    + "\">Statistics</a> | <a href=\"charts.html?service=" + service
                    + "\">Charts</a>", "Providers (" + rows.size() + ")",
                    new String[]{"Provider URL:", "Unregister"}, rows);
        } else if (host != null && host.length() > 0) {
            List<List<String>> rows = new ArrayList<List<String>>();
            List<URL> providers = RegistryContainer.getInstance().getProvidersByHost(host);
            if (providers != null && providers.size() > 0) {
                for (URL u : providers) {
                    List<String> row = new ArrayList<String>();
                    String s = u.toFullString();
                    row.add(s.replace("&", "&amp;"));
                    row.add("<button onclick=\"if(confirm('Confirm unregister provider?')){window.location.href='unregister.html?host=" + host + "&provider=" + URL.encode(s) + "';}\">Unregister</button>");
                    rows.add(row);
                }
            }
            return new Page("<a href=\"hosts.html\">Hosts</a> &gt; " + NetUtils.getHostName(host) + "/" + host + " &gt; Providers | <a href=\"consumers.html?host=" + host + "\">Consumers</a>", "Providers (" + rows.size() + ")",
                    new String[]{"Provider URL:", "Unregister"}, rows);
        } else if (application != null && application.length() > 0) {
            List<List<String>> rows = new ArrayList<List<String>>();
            List<URL> providers = RegistryContainer.getInstance().getProvidersByApplication(application);
            if (providers != null && providers.size() > 0) {
                for (URL u : providers) {
                    List<String> row = new ArrayList<String>();
                    String s = u.toFullString();
                    row.add(s.replace("&", "&amp;"));
                    row.add("<button onclick=\"if(confirm('Confirm unregister provider?')){window.location.href='unregister.html?application=" + application + "&provider=" + URL.encode(s) + "';}\">Unregister</button>");
                    rows.add(row);
                }
            }
            return new Page("<a href=\"applications.html\">Applications</a> &gt; " + application + " &gt; Providers | <a href=\"consumers.html?application=" + application + "\">Consumers</a> | <a href=\"dependencies.html?application=" + application + "\">Depends On</a> | <a href=\"dependencies.html?application=" + application + "&reverse=true\">Used By</a>", "Providers (" + rows.size() + ")",
                    new String[]{"Provider URL:", "Unregister"}, rows);
        } else {
            throw new IllegalArgumentException("Please input service or host or application parameter.");
        }
    }

}