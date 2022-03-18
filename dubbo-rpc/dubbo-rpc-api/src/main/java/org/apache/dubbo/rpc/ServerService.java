package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

public interface ServerService<T> {

    Invoker<T> getInvoker(URL url);

    ServiceDescriptor getServiceDescriptor();

}
