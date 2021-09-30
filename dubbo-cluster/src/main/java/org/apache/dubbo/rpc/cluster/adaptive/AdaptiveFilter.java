package org.apache.dubbo.rpc.cluster.adaptive;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.loadbalance.P2CLoadBalance;

import java.util.function.Supplier;

@Activate(group = CommonConstants.CONSUMER)
public class AdaptiveFilter implements Filter, BaseFilter.Listener {
    private static final Supplier<Long> clock = System::nanoTime;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            Result result = invoker.invoke(invocation);
            return result;
        } catch (Exception e) {
            throw e;
        }

    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        String remain = appResponse.getAttachment("remain");
        String limit = appResponse.getAttachment("limit");
        long pickTime = (Long)invocation.get("pickTime");
        P2CLoadBalance.updateNodes(Integer.parseInt(remain),invocation.getMethodName(),invoker.getUrl().getBackupAddress(),clock.get() - pickTime);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {

    }
}
