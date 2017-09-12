package com.alibaba.dubbo.common.extension.support;

/**
 * @author zhenyu.nie created on 2016 2016/11/25 20:42
 */
public class HasCircleException extends RuntimeException {

    public HasCircleException(String lName, String rName) {
        super("activates has a circle with [" + lName + "] and [" + rName + "]");
    }
}
