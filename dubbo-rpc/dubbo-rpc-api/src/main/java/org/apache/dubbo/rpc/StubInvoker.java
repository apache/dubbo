package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.proxy.AbstractProxyInvoker;

import java.util.Map;
import java.util.function.Function;

public class StubInvoker<T> extends AbstractProxyInvoker<T> {
    public final Class<T> type;
    public final URL url;
    private final Map<String, Function<Object[], Object>> handlers;

    public StubInvoker(T impl, URL url, Class<T> type, Map<String, Function<Object[], Object>> handlers) {
        super(impl, type, url);
        this.url = url;
        this.type = type;
        this.handlers = handlers;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {
    }

    @Override
    public Class<T> getInterface() {
        return type;
    }

    @Override
    protected Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable {
        return handlers.get(methodName).apply(arguments);
    }
}
