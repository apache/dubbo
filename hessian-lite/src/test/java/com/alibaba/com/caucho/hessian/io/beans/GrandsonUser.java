package com.alibaba.com.caucho.hessian.io.beans;

import java.io.Serializable;

/**
 * @author WangXin
 */
public class GrandsonUser extends SubUser implements Serializable {
    private static final long serialVersionUID = 5013145666993778451L;
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
