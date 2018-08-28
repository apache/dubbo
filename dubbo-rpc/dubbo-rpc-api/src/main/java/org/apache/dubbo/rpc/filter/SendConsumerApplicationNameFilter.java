package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * @author pfjia
 * @since 2018/8/27 22:29
 */
@Activate(group = Constants.CONSUMER)
public class SendConsumerApplicationNameFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (invocation instanceof RpcInvocation) {
            String consumerApplicationName = invoker.getUrl().getParameter(Constants.APPLICATION_KEY);
            ((RpcInvocation) invocation).setAttachment(Constants.SEND_CONSUMER_APPLICATION_NAME_KEY, consumerApplicationName);
        }
        return invoker.invoke(invocation);
    }
}
