package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invoker;

@SPI("triple")
public interface InvokerResolver {

    void add(String path, String service, Invoker<?> invoker);

    Invoker<?> resolve(String path);
}
