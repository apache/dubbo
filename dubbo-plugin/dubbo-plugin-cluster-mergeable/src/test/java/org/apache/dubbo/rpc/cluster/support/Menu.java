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
package org.apache.dubbo.rpc.cluster.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Menu {

    private Map<String, List<String>> menus = new HashMap<String, List<String>>();

    public Menu() {
    }

    public Menu(Map<String, List<String>> menus) {
        for (Map.Entry<String, List<String>> entry : menus.entrySet()) {
            this.menus.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }
    }

    public void putMenuItem(String menu, String item) {
        List<String> items = menus.get(menu);
        if (item == null) {
            items = new ArrayList<String>();
            menus.put(menu, items);
        }
        items.add(item);
    }

    public void addMenu(String menu, List<String> items) {
        List<String> menuItems = menus.get(menu);
        if (menuItems == null) {
            menus.put(menu, new ArrayList<String>(items));
        } else {
            menuItems.addAll(new ArrayList<String>(items));
        }
    }

    public Map<String, List<String>> getMenus() {
        return Collections.unmodifiableMap(menus);
    }

    public void merge(Menu menu) {
        for (Map.Entry<String, List<String>> entry : menu.menus.entrySet()) {
            addMenu(entry.getKey(), entry.getValue());
        }
    }

}
