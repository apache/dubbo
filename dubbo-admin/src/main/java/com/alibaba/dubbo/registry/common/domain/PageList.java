/**
 * Project: dubbo.registry.server-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-7-7
 * <p>
 * Copyright 1999-2010 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.registry.common.domain;

import java.io.Serializable;
import java.util.List;

/**
 * PageList
 *
 * @author william.liangf
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
