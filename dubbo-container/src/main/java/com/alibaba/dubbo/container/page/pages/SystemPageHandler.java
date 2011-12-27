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
package com.alibaba.dubbo.container.page.pages;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;

/**
 * SystemPageHandler
 * 
 * @author william.liangf
 */
@Extension("system")
public class SystemPageHandler implements PageHandler {

    public Page handle(URL url) {
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();
        row.add("OS");
        row.add(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        rows.add(row);
        row = new ArrayList<String>();
        row.add("JVM");
        row.add(System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version") + ",<br/>" + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " " + System.getProperty("java.vm.info", ""));
        rows.add(row);
        row = new ArrayList<String>();
        row.add("CPU");
        row.add(System.getProperty("os.arch", "") + ", " + String.valueOf(Runtime.getRuntime().availableProcessors()) + " cores");
        rows.add(row);
        row = new ArrayList<String>();
        row.add("Time");
        row.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").format(new Date()));
        rows.add(row);
        row = new ArrayList<String>();
        row.add("Locale");
        row.add(Locale.getDefault().toString() + "/" + System.getProperty("file.encoding"));
        rows.add(row);
        return new Page("<a href=\"/\">Home</a> &gt; System", "System", new String[] {
                "Name", "Value" }, rows);
    }

}