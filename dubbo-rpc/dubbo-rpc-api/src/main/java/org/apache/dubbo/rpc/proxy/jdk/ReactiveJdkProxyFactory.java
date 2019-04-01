package org.apache.dubbo.rpc.proxy.jdk;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.proxy.AbstractReactiveProxyFactory;
import org.apache.dubbo.rpc.proxy.ReactiveInvokerInvocationHandler;

import java.lang.reflect.Proxy;

/**
 * Reactive implementation of JdkProxyFactory
 * @author cherry
 */
public class ReactiveJdkProxyFactory extends AbstractReactiveProxyFactory {
    public static final String NAME = "reactivejdk";

    @Override
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                interfaces, new ReactiveInvokerInvocationHandler(invoker));
    }
}
