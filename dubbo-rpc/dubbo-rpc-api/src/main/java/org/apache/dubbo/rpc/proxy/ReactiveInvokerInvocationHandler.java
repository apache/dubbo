package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.apache.dubbo.common.Constants.KEY_PUBLISHER_TYPE;
import static org.apache.dubbo.common.Constants.VALUE_PUBLISHER_MONO;
import static org.apache.dubbo.common.Constants.VALUE_PUBLISHER_FLUX;

/**
 * Reactive implementation of InvokerInvocationHandler which actually communicate with the real providers.
 * @author cherry
 */
public class ReactiveInvokerInvocationHandler extends InvokerInvocationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveInvokerInvocationHandler.class);
    private final Invoker<?> invoker;
    public ReactiveInvokerInvocationHandler(Invoker<?> handler) {
        super(handler);
        this.invoker = handler;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //if the invocation returns a publisher,make a publisher wrapping the real invocation
        Class returnType = method.getReturnType();
        if (Publisher.class.isAssignableFrom(returnType)) {
            RpcInvocation invocation = createInvocation(method, args);
            if (Mono.class.isAssignableFrom(returnType)) {
                invocation.setAttachment(KEY_PUBLISHER_TYPE,VALUE_PUBLISHER_MONO);
                return Mono.create(monoSink -> {
                    try {
                        CompletableFuture<Object> future
                                = (CompletableFuture<Object>) invoker.invoke(invocation).recreate();
                        future.whenComplete((v, t) -> {
                            if (t != null) {
                                monoSink.error(t);
                            } else {
                                monoSink.success(v);
                            }
                        });
                    } catch (Throwable throwable) {
                        if(LOGGER.isErrorEnabled()) {
                            LOGGER.error("mono invocation",throwable);
                        }
                        monoSink.error(throwable);
                    }
                });
            } else if (Flux.class.isAssignableFrom(returnType)) {
                invocation.setAttachment(KEY_PUBLISHER_TYPE,VALUE_PUBLISHER_FLUX);
                return Flux.create(fluxSink -> {
                    try {
                        CompletableFuture<Object> future
                                = (CompletableFuture<Object>) invoker.invoke(invocation).recreate();
                        future.whenComplete((v, t) -> {
                            if (t != null) {
                                fluxSink.error(t);
                            } else if (v instanceof List){
                                List list = (List) v;
                                if(list!=null) {
                                    list.forEach(fluxSink::next);
                                }
                                fluxSink.complete();
                            } else {
                                Exception ex = new IllegalArgumentException("unexpected return type:"+v.getClass());
                                fluxSink.error(ex);
                            }
                        });
                    } catch (Throwable throwable) {
                        if(LOGGER.isErrorEnabled()) {
                            LOGGER.error("flux invocation",throwable);
                        }
                        fluxSink.error(throwable);
                    }
                });
            } else {
                //TODO other publishers support
                throw new IllegalArgumentException(
                        String.format("%s not supported now", method.getReturnType().getSimpleName()));
            }
        }
        return super.invoke(proxy, method, args);
    }

    protected RpcInvocation createInvocation(Method method, Object[] args) {
        RpcInvocation invocation = new RpcInvocation(method, args);
        invocation.setAttachment(Constants.FUTURE_RETURNTYPE_KEY, "true");
        invocation.setAttachment(Constants.ASYNC_KEY, "true");
        return invocation;
    }
}
