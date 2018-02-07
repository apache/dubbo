package com.alibaba.com.caucho.hessian.io.beans;

import java.io.Serializable;

/**
 */
public class SubUser extends BaseUser implements Serializable {
    private static final long serialVersionUID = 4017613093053853415L;
    private String userName;

    @Override
    public String getUserName() {
        return userName;
    }
    @Override
    public void setUserName(String userName) {
        this.userName = userName;
    }
}
