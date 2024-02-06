package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.directory.XdsDirectory;

import java.util.List;

public class XdsClusterInvoker<T> extends AbstractClusterInvoker<T>{

    private XdsDirectory xdsDirectory;

    public XdsClusterInvoker(XdsDirectory xdsDirectory){
        this.xdsDirectory = xdsDirectory;
    }

    @Override
    protected Result doInvoke(Invocation invocation, List list, LoadBalance loadbalance) throws RpcException {
        return null;
    }

}
