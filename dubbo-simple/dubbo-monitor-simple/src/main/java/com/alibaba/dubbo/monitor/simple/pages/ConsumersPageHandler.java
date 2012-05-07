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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.monitor.simple.RegistryContainer;

/**
 * ConsumersPageHandler
 * 
 * @author william.liangf
 */
public class ConsumersPageHandler implements PageHandler {
    
    public Page handle(URL url) {
        String service = url.getParameter("service");
        String host = url.getParameter("host");
        String application = url.getParameter("application");
        if (service != null && service.length() > 0) {
            List<List<String>> rows = new ArrayList<List<String>>();
            List<URL> consumers = RegistryContainer.getInstance().getConsumersByService(service);
            if (consumers != null && consumers.size() > 0) {
                for (URL u : consumers) {
                    List<String> row = new ArrayList<String>();
                    String s = u.toFullString();
                    row.add(s.replace("&", "&amp;"));
                    row.add("<button onclick=\"if(confirm('Confirm unsubscribe consumer?')){window.location.href='unsubscribe.html?service=" + service + "&consumer=" + URL.encode(s) + "';}\">Unsubscribe</button>");
                    rows.add(row);
                }
            }
            return new Page("<a href=\"services.html\">Services</a> &gt; " + service 
                    + " &gt; <a href=\"providers.html?service=" + service 
                    + "\">Providers</a> | Consumers | <a href=\"statistics.html?service=" + service 
                    + "\">Statistics</a> | <a href=\"charts.html?service=" + service 
                    + "\">Charts</a>", "Consumers (" + rows.size() + ")",
                    new String[] { "Consumer URL:", "Unsubscribe" }, rows);
        } else if (host != null && host.length() > 0) {
            List<List<String>> rows = new ArrayList<List<String>>();
            List<URL> consumers = RegistryContainer.getInstance().getConsumersByHost(host);
            if (consumers != null && consumers.size() > 0) {
                for (URL u : consumers) {
                    List<String> row = new ArrayList<String>();
                    String s = u.toFullString();
                    row.add(s.replace("&", "&amp;"));
                    row.add("<button onclick=\"if(confirm('Confirm unsubscribe consumer?')){window.location.href='unsubscribe.html?host=" + host + "&consumer=" + URL.encode(s) + "';}\">Unsubscribe</button>");
                    rows.add(row);
                }
            }
            return new Page("<a href=\"hosts.html\">Hosts</a> &gt; " + NetUtils.getHostName(host) + "/" + host + " &gt; <a href=\"providers.html?host=" + host + "\">Providers</a> | Consumers", "Consumers (" + rows.size() + ")",
                    new String[] { "Consumer URL:", "Unsubscribe" }, rows);
        } else if (application != null && application.length() > 0) {
            List<List<String>> rows = new ArrayList<List<String>>();
            List<URL> consumers = RegistryContainer.getInstance().getConsumersByApplication(application);
            if (consumers != null && consumers.size() > 0) {
                for (URL u : consumers) {
                    List<String> row = new ArrayList<String>();
                    String s = u.toFullString();
                    row.add(s.replace("&", "&amp;"));
                    row.add("<button onclick=\"if(confirm('Confirm unsubscribe consumer?')){window.location.href='unsubscribe.html?application=" + application + "&consumer=" + URL.encode(s) + "';}\">Unsubscribe</button>");
                    rows.add(row);
                }
            }
            return new Page("<a href=\"applications.html\">Applications</a> &gt; " + application + " &gt; <a href=\"providers.html?application=" + application + "\">Providers</a> | Consumers | <a href=\"dependencies.html?application=" + application + "\">Depends On</a> | <a href=\"dependencies.html?application=" + application + "&reverse=true\">Used By</a>", "Consumers (" + rows.size() + ")",
                    new String[] { "Consumer URL:", "Unsubscribe" }, rows);
        } else {
            throw new IllegalArgumentException("Please input service or host or application parameter.");
        }
    }

}