package com.alibaba.dubbo.qos.common;

/**
 * @author qinliujie
 * @date 2017/11/17
 */
public interface Constants {

    int DEFAULT_PORT = 22222;
    // 通过-D参数指定port
    String QOS_PORT = "dubbo.qos.port";
    String BR_STR = "\r\n";
    String CLOSE = "close!";

    // 通过-D参数指定当前qos是否能够拒绝外部ip的联入
    String ACCEPT_FOREIGN_IP = "dubbo.qos.accept.foreign.ip";
}
