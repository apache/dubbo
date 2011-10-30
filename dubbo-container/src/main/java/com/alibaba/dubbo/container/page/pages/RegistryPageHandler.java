/**
 * Project: dubbo.core.service.server-1.0.6-SNAPSHOT
 * 
 * File Created at 2010-1-19
 * $Id: RegistryInformationProvider.java 39071 2010-03-22 03:39:59Z william.liangf $
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;

/**
 * RegistryPageHandler
 * 
 * @author william.liangf
 */
@Extension("registry")
public class RegistryPageHandler implements PageHandler {

    public Page handle(URL url) {
        List<List<String>> rows = new ArrayList<List<String>>();
        Collection<Registry> registries = AbstractRegistryFactory.getRegistries();
        if (registries != null && registries.size() > 0) {
            int i = 0;
            for (Registry registry : registries) {
                i ++;
                String server = registry.getUrl().getAddress();
                List<String> row = new ArrayList<String>();
                row.add(String.valueOf(i));
                row.add(server);
                if (registry.isAvailable()) {
                    row.add("Connected");
                } else {
                    row.add("");
                }
                rows.add(row);
            }
        }
        return new Page("<a href=\"/\">Home</a> &gt; Registry", "Registries (" + rows.size() + ")",
                new String[] { "Server Group:", "Server Address:", "Is Connected" }, rows);
    }

}
