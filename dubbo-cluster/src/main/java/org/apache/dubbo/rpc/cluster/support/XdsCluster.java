package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.directory.XdsDirectory;
import org.apache.dubbo.rpc.cluster.support.wrapper.AbstractCluster;

public class XdsCluster extends AbstractCluster {

    public static final String NAME = "xds";

    @Override
    protected <T> AbstractClusterInvoker<T> doJoin(Directory<T> directory) throws RpcException {
        XdsDirectory<T> xdsDirectory = new XdsDirectory<>(directory);
        return new XdsClusterInvoker<>(xdsDirectory);
    }
}
