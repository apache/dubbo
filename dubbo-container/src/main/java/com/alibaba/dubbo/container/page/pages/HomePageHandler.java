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