package com.alibaba.dubbo.rpc.protocol.ice;

import Ice.Communicator;
import Ice.Util;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;

/**
 * Created by wuyu on 2017/2/4.
 */
public class IceProtocol extends AbstractProxyProtocol {
    @Override
    public int getDefaultPort() {
        return 30110;
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int connections = url.getParameter(Constants.THREADPOOL_KEY, 200);
        Communicator ic = Util.initialize();
        final Ice.ObjectAdapter adapter = ic.createObjectAdapterWithEndpoints("dubbo-ice", "default -p " + url.getPort());
        adapter.add((Ice.Object) impl, ic.stringToIdentity(type.getName()));
        adapter.activate();
        ic.getProperties().setProperty("IceBox.ThreadPool.Server.Size", "5");
        ic.getProperties().setProperty("IceBox.ThreadPool.Server.SizeMax", "" + connections);
//        IceBox.ThreadPool.Client.Size=4;
//        IceBox.ThreadPool.Client.SizeMax=100;
//        IceBox.ThreadPool.Client.SizeWarn=40;
        return new Runnable() {
            @Override
            public void run() {
                adapter.destroy();
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        return null;
    }
}
