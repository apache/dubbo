package com.alibaba.com.caucho.hessian.io.beans;

import java.io.Serializable;

/**
 * @author WangXin
 */
public class BaseUser implements Serializable {
    private static final long serialVersionUID = 9104092580669691633L;
    private Integer userId;
    private String userName;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
