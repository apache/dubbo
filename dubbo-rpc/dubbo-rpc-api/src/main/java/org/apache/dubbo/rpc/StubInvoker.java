package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.profiler.Profiler;
import org.apache.dubbo.common.profiler.ProfilerEntry;
import org.apache.dubbo.common.profiler.ProfilerSwitch;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

public class StubInvoker<T> implements Invoker<T> {
    private static final Logger logger = LoggerFactory.getLogger(StubInvoker.class);

    public final Class<T> type;
    public final URL url;
    private final Map<String, Function<Object[], CompletableFuture<?>>> handlers;

    public StubInvoker(URL url, Class<T> type, Map<String, Function<Object[], CompletableFuture<?>>> handlers) {
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
    public Result invoke(Invocation invocation) throws RpcException {
        ProfilerEntry originEntry = null;
        if (ProfilerSwitch.isEnableSimpleProfiler()) {
            Object fromInvocation = invocation.get(Profiler.PROFILER_KEY);
            if (fromInvocation instanceof ProfilerEntry) {
                ProfilerEntry profiler = Profiler.enter((ProfilerEntry) fromInvocation,
                    "Receive request. Server biz impl invoke begin.");
                invocation.put(Profiler.PROFILER_KEY, profiler);
                originEntry = Profiler.setToBizProfiler(profiler);
            }
        }

        CompletableFuture<?> future = handlers.get(invocation.getMethodName())
            .apply(invocation.getArguments());

        if (ProfilerSwitch.isEnableSimpleProfiler()) {
            Object fromInvocation = invocation.get(Profiler.PROFILER_KEY);
            if (fromInvocation instanceof ProfilerEntry) {
                ProfilerEntry profiler = Profiler.release((ProfilerEntry) fromInvocation);
                invocation.put(Profiler.PROFILER_KEY, profiler);
            }
        }
        Profiler.removeBizProfiler();
        if (originEntry != null) {
            Profiler.setToBizProfiler(originEntry);
        }
        CompletableFuture<AppResponse> appResponseFuture = future.handle((obj, t) -> {
            AppResponse result = new AppResponse(invocation);
            if (t != null) {
                if (t instanceof CompletionException) {
                    result.setException(t.getCause());
                } else {
                    result.setException(t);
                }
            } else {
                result.setValue(obj);
            }
            return result;
        });
        return new AsyncRpcResult(appResponseFuture, invocation);
    }
}
