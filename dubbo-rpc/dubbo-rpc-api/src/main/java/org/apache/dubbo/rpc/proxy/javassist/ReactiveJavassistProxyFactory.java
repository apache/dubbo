package org.apache.dubbo.rpc.proxy.javassist;

import org.apache.dubbo.common.bytecode.Proxy;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.proxy.AbstractReactiveProxyFactory;
import org.apache.dubbo.rpc.proxy.ReactiveInvokerInvocationHandler;

/**
 * Reactive implementation of JavassistProxyFactory
 * @author cherry
 */
public class ReactiveJavassistProxyFactory extends AbstractReactiveProxyFactory {
    public static final String NAME = "reactivejavassist";

    @Override
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.getProxy(interfaces).newInstance(new ReactiveInvokerInvocationHandler(invoker));
    }

}
