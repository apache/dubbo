/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.registry.common.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * DependItem
 *
 * @author william.liangf
 */
public class DependItem {

    private final List<Integer> recursives = new ArrayList<Integer>();
    private String application;
    private int index;
    private int level;
    private DependItem parent;

    public DependItem() {
    }

    public DependItem(String application, int level) {
        this.application = application;
        this.level = level;
    }

    public DependItem(DependItem parent, String application, int level, int index) {
        this.parent = parent;
        this.application = application;
        this.level = level;
        this.index = index;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public DependItem getParent() {
        return parent;
    }

    public void setParent(DependItem parent) {
        this.parent = parent;
    }

    public List<Integer> getRecursives() {
        return recursives;
    }

    public void addRecursive(int padding, int value) {
        while (recursives.size() < padding) {
            recursives.add(0);
        }
        recursives.add(value);
    }

    public String toString() {
        return "DependItem [application=" + application + ", index=" + index + ", level=" + level
                + "]";
    }

}
