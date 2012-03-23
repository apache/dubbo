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
import java.util.List;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.container.page.Menu;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.container.page.PageServlet;

/**
 * HomePageHandler
 * 
 * @author william.liangf
 */
@Menu(name = "Home", desc = "Home page.", order = Integer.MIN_VALUE)
public class HomePageHandler implements PageHandler {

    public Page handle(URL url) {
        List<List<String>> rows = new ArrayList<List<String>>();
        for (PageHandler handler : PageServlet.getInstance().getMenus()) {
            String uri = ExtensionLoader.getExtensionLoader(PageHandler.class).getExtensionName(handler);
            Menu menu = handler.getClass().getAnnotation(Menu.class);
            List<String> row = new ArrayList<String>();
            row.add("<a href=\"" + uri + ".html\">" + menu.name() + "</a>");
            row.add(menu.desc());
            rows.add(row);
        }
        return new Page("Home", "Menus",  new String[] {"Menu Name", "Menu Desc"}, rows);
    }

}