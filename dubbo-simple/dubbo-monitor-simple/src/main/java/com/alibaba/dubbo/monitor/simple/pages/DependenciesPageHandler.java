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
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.monitor.simple.RegistryContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DependenciesPageHandler
 */
public class DependenciesPageHandler implements PageHandler {

    public Page handle(URL url) {
        String application = url.getParameter("application");
        if (application == null || application.length() == 0) {
            throw new IllegalArgumentException("Please input application parameter.");
        }
        boolean reverse = url.getParameter("reverse", false);
        List<List<String>> rows = new ArrayList<List<String>>();
        Set<String> directly = RegistryContainer.getInstance().getDependencies(application, reverse);
        Set<String> indirectly = new HashSet<String>();
        appendDependency(rows, reverse, application, 0, new HashSet<String>(), indirectly);
        indirectly.remove(application);
        return new Page("<a href=\"applications.html\">Applications</a> &gt; " + application +
                " &gt; <a href=\"providers.html?application=" + application + "\">Providers</a> | <a href=\"consumers.html?application=" + application + "\">Consumers</a> | " +
                (reverse ? "<a href=\"dependencies.html?application=" + application + "\">Depends On</a> | Used By"
                        : "Depends On | <a href=\"dependencies.html?application=" + application + "&reverse=true\">Used By</a>"), (reverse ? "Used By" : "Depends On") + " (" + directly.size() + "/" + indirectly.size() + ")", new String[]{"Application Name:"}, rows);
    }

    private void appendDependency(List<List<String>> rows, boolean reverse, String application, int level, Set<String> appended, Set<String> indirectly) {
        List<String> row = new ArrayList<String>();
        StringBuilder buf = new StringBuilder();
        if (level > 0) {
            for (int i = 0; i < level; i++) {
                buf.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|");
            }
            buf.append(reverse ? "&lt;-- " : "--&gt; ");
        }
        boolean end = false;
        if (level > 5) {
            buf.append(" <font color=\"blue\">More...</font>");
            end = true;
        } else {
            buf.append(application);
            if (appended.contains(application)) {
                buf.append(" <font color=\"red\">(Cycle)</font>");
                end = true;
            }
        }
        row.add(buf.toString());
        rows.add(row);
        if (end) {
            return;
        }

        appended.add(application);
        indirectly.add(application);
        Set<String> dependencies = RegistryContainer.getInstance().getDependencies(application, reverse);
        if (dependencies != null && dependencies.size() > 0) {
            for (String dependency : dependencies) {
                appendDependency(rows, reverse, dependency, level + 1, appended, indirectly);
            }
        }
        appended.remove(application);
    }

}