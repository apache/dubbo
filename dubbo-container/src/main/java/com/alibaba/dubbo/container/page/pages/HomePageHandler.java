/**
 * Project: dubbo.core.service.server-1.0.6-SNAPSHOT
 * 
 * File Created at 2010-1-19
 * $Id: HomeInformationProvider.java 34685 2010-01-19 07:32:31Z william.liangf $
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
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;

/**
 * HomePageHandler
 * 
 * @author william.liangf
 */
@Extension("home")
public class HomePageHandler implements PageHandler {

    public Page handle(URL url) {
        Collection<String> informationProviders = ExtensionLoader.getExtensionLoader(PageHandler.class).getSupportedExtensions();
        List<List<String>> rows = new ArrayList<List<String>>();
        for (String uri : informationProviders) {
            List<String> row = new ArrayList<String>();
            row.add("<a href=\"" + uri + ".html\">" + uri + "</a>");
            rows.add(row);
        }
        return new Page("Home", "Menus",  new String[] {"Menu"}, rows);
    }

}
