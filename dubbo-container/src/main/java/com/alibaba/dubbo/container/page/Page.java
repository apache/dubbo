/**
 * Project: dubbo.core.service.server-1.0.6-SNAPSHOT
 * 
 * File Created at 2010-1-19
 * $Id: Information.java 34672 2010-01-19 06:25:44Z william.liangf $
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
package com.alibaba.dubbo.container.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Information
 * 
 * @author william.liangf
 */
public class Page {

    private final String navigation;
    
    private final String title;

    private final List<String> columns;

    private final List<List<String>> rows;

    public Page(String navigation, String title,
                       String column, String row) {
        this(navigation, title, column == null ? null : Arrays.asList(new String[]{column}), row == null ? null : stringToList(row));
    }
    
    private static List<List<String>> stringToList(String str) {
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();
        row.add(str);
        rows.add(row);
        return rows;
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
