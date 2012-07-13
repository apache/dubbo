/**
 * Function: dubbo用户类
 * 
 * File Created at 2011-08-17
 * 
 * Copyright 2011 Alibaba.com Croporation Limited.
 * All rights reserved.
 */
package com.alibaba.dubbo.governance.web.common.auth;

import java.io.Serializable;

import com.alibaba.dubbo.registry.common.domain.User;

/**
 * MinasUser: DubboUser
 * 
 * @author guanghui.shigh
 */
public class DubboUser implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final ThreadLocal<User> userHolder = new ThreadLocal<User>();

	private DubboUser() {
	}

    public static final User getCurrentUser() {
        return (User) userHolder.get();
    }

    public static final void setCurrentUser(User user) {
        userHolder.set(user);
    }

}
