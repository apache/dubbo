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
package com.alibaba.dubbo.container.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Page
 */
public class Page {

    private final String navigation;

    private final String title;

    private final List<String> columns;

    private final List<List<String>> rows;

    public Page(String navigation) {
        this(navigation, (String) null, (String[]) null, (List<List<String>>) null);
    }

    public Page(String navigation, String title,
                String column, String row) {
        this(navigation, title, column == null ? null : Arrays.asList(new String[]{column}), row == null ? null : stringToList(row));
    }

    public Page(String navigation, String title,
                String[] columns, List<List<String>> rows) {
        this(navigation, title, columns == null ? null : Arrays.asList(columns), rows);
    }

    public Page(String navigation, String title,
                List<String> columns, List<List<String>> rows) {
        this.navigation = navigation;
        this.title = title;
        this.columns = columns;
        this.rows = rows;
    }

    private static List<List<String>> stringToList(String str) {
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();
        row.add(str);
        rows.add(row);
        return rows;
    }

    public String getNavigation() {
        return navigation;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<List<String>> getRows() {
        return rows;
    }

}
