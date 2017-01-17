package com.alibaba.dubbo.rpc.listener;

import com.alibaba.dubbo.common.extension.SPI;

/**
 * Created by wuyu on 2017/1/17.
 */
@SPI("protocolListener")
public interface ProtocolListener {

    public void start();
}
