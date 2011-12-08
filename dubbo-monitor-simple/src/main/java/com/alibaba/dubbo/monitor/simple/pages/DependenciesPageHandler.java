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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.monitor.simple.RegistryContainer;

/**
 * DependenciesPageHandler
 * 
 * @author william.liangf
 */
@Extension("dependencies")
public class DependenciesPageHandler implements PageHandler {
    
    public Page handle(URL url) {
        String application = url.getParameter("application");
        if (application == null || application.length() == 0) {
            throw new IllegalArgumentException("Please input application parameter.");
        }
        boolean afferent = "afferent".equals(url.getParameter("direction"));
        List<List<String>> rows = new ArrayList<List<String>>();
        appendDependency(rows, afferent, application, 0, new HashSet<String>());
        return new Page("<a href=\"applications.html\">Applications</a> &gt; " + application + 
                (afferent ? " &gt; <a href=\"dependencies.html?direction=efferent&application=" + application + "\">Efferent Dependencies</a> | Afferent Dependencies" 
                        : " &gt; Efferent Dependencies | <a href=\"dependencies.html?direction=afferent&application=" + application + "\">Afferent Dependencies</a>"), (afferent ? "Afferent Dependencies" : "Efferent Dependencies") + " (" + rows.size() + ")", new String[] { "Application Name:"}, rows);
    }
    
    private void appendDependency(List<List<String>> rows, boolean afferent, String application, int level, Set<String> appended) {
        List<String> row = new ArrayList<String>();
        StringBuilder buf = new StringBuilder();
        if (level > 0) {
            for (int i = 0; i < level; i ++) {
                buf.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            }
            buf.append(afferent ? "&lt;-- " : "--&gt; ");
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
        Set<String> dependencies = RegistryContainer.getInstance().getDependencies(application, afferent);
        if (dependencies != null && dependencies.size() > 0) {
            for (String dependency : dependencies) {
                appendDependency(rows, afferent, dependency, level + 1, appended);
            }
        }
        appended.remove(application);
    }

}
