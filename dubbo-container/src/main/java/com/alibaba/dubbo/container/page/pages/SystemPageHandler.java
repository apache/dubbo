/**
 * Project: dubbo.core.service.server-1.0.6-SNAPSHOT
 * 
 * File Created at 2010-1-19
 * $Id: SystemInformationProvider.java 36098 2010-02-01 08:19:31Z william.liangf $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
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
