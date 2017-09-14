/**
 * Project: dubbo.registry.common-2.1.0-SNAPSHOT
 * <p>
 * File Created at Dec 9, 2011
 * $Id: ToolTest.java 181192 2012-06-21 05:05:47Z tony.chenl $
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
package com.alibaba.dubbo.registry.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author ding.lid
 */
public class ToolTest {
    @Test
    public void test_getSimpleName() throws Exception {
        assertEquals("cn/MemberService:1.0.0", Tool.getSimpleName("cn/com.alibaba.morgan.MemberService:1.0.0"));
        assertEquals("cn/MemberService", Tool.getSimpleName("cn/com.alibaba.morgan.MemberService"));
        assertEquals("MemberService:1.0.0", Tool.getSimpleName("com.alibaba.morgan.MemberService:1.0.0"));
        assertEquals("MemberService", Tool.getSimpleName("com.alibaba.morgan.MemberService"));
    }
}
