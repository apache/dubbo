package com.alibaba.dubbo.qos.command;

/**
 * @author qinliujie
 * @date 2017/11/17
 */
public class NoSuchCommandException extends Exception {
    public NoSuchCommandException(String msg) {
        super("NoSuchCommandException:" + msg);
    }
}
