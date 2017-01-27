package com.alibaba.dubbo.rpc.protocol.redis2;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;

/**
 * Created by wuyu on 2017/1/26.
 */
public class Redis2Protocol extends AbstractProxyProtocol{


    @Override
    public int getDefaultPort() {
        return 6381;
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        return null;
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        return null;
    }
}
