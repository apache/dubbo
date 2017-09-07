/**
 * Project: dubbo.registry.server-2.1.0-SNAPSHOT
 * <p>
 * File Created at Oct 31, 2011
 * $Id: PageListTest.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * <p>
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.registry.common.domain;

import org.junit.Assert;
import org.junit.Test;

/**
 * TODO Comment of PageListTest
 * @author haomin.liuhm
 *
 */
public class PageListTest {

    @Test
    public void testGetPageCount() {
        //int start, int limit, int total, List<T> list
        PageList<Object> pl = new PageList<Object>(0, 100, 52, null);
        Assert.assertEquals(1, pl.getPageCount());

        pl = new PageList<Object>(0, -100, -3, null);
        Assert.assertEquals(1, pl.getPageCount());

        pl = new PageList<Object>(0, 30, 100, null);
        Assert.assertEquals(4, pl.getPageCount());
    }

}
