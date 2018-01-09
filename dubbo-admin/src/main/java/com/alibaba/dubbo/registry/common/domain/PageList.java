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
package com.alibaba.dubbo.registry.common.domain;

import java.io.Serializable;
import java.util.List;

/**
 * PageList
 *
 */
public class PageList<T> implements Serializable {

    private static final long serialVersionUID = 43869560130672722L;

    private int start;

    private int limit;

    private int total;

    private List<T> list;

    public PageList() {
    }

    public PageList(int start, int limit, int total, List<T> list) {
        this.start = start;
        this.limit = limit;
        this.total = total;
        this.list = list;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public int getPageCount() {

        int lim = limit;
        if (limit < 1) {
            lim = 1;
        }

        int page = total / lim;
        if (page < 1) {
            return 1;
        }

        int remain = total % lim;

        if (remain > 0) {
            page += 1;
        }

        return page;
    }

}
