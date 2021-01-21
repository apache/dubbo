package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invoker;

@SPI(CommonConstants.TRIPLE)
public interface PathResolver {

    void add(String path, Invoker<?> invoker);

    Invoker<?> resolve(String path);

    void remove(String path);

    void destroy();
}
