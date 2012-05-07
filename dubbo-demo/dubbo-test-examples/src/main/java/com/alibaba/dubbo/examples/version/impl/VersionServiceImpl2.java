/**
 * Project: dubbo-examples
 * 
 * File Created at 2012-2-17
 * $Id$
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.examples.version.impl;

import com.alibaba.dubbo.examples.version.api.VersionService;

/**
 * @author william.liangf
 */
public class VersionServiceImpl2 implements VersionService {

    public String sayHello(String name) {
        return "hello2, " + name;
    }

}
