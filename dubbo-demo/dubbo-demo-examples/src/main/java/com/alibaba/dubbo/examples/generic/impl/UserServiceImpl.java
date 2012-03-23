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
package com.alibaba.dubbo.examples.generic.impl;

import com.alibaba.dubbo.examples.generic.api.IUserService;

/**
 * @author chao.liuc
 *
 */
public class UserServiceImpl implements IUserService {

    public User get(Params params) {
        return new User(1, "charles");
    }
}
