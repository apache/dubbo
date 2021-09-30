package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.filter.limiter.AbstractLimiter;
import org.apache.dubbo.rpc.filter.limiter.Limiter;
import org.apache.dubbo.rpc.filter.limiter.SimpleLimiter;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 服务端过滤器
 * 可选接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 用户可以在服务端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = CommonConstants.PROVIDER)
public class AdaptiveFilter implements Filter, BaseFilter.Listener {
    private static final ConcurrentHashMap<String, Limiter> name2limiter = new ConcurrentHashMap<>();
    private static Supplier<Long> clock = System::nanoTime;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            String methodName = invocation.getMethodName();
            name2limiter.computeIfAbsent(methodName,(name) ->{
                return new SimpleLimiter();
            });
            AbstractLimiter limiter = (AbstractLimiter)name2limiter.get(methodName);
            //acquire
            Optional<Limiter.Listener> listener = limiter.acquire();
            if (!listener.isPresent()){
                throw new RpcException(RpcException.LIMIT_EXCEEDED_EXCEPTION,
                        "Waiting concurrent invoke timeout in client-side for service:  " +
                                invoker.getInterface().getName() + ", method: " + invocation.getMethodName() +
                                ", limit: " + limiter.getLimit());
            }
            invocation.put("adaptive_listener",listener);
            Result result = invoker.invoke(invocation);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        //get limiter
        AbstractLimiter limiter = (AbstractLimiter)name2limiter.get(invocation.getMethodName());
        appResponse.setAttachment("remain",String.valueOf(limiter.getRemain()));
        appResponse.setAttachment("limit",String.valueOf(limiter.getLimit()));
        //get listener
        Optional<Limiter.Listener> listener = (Optional<Limiter.Listener>) invocation.get("adaptive_listener");
        listener.get().onSuccess();
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        //get listener
        Optional<Limiter.Listener> listener = (Optional<Limiter.Listener>) invocation.get("adaptive_listener");
        if (!listener.isPresent()){
            listener.get().onDroped();
        }
    }
}
