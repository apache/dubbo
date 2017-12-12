package com.alibaba.dubbo.rpc.protocol.hessian.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.protocol.hessian.HessianProtocol;

@Activate(group = Constants.PROVIDER)
public class HessianFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        //判断是否是hessian协议
        if (invoker.getUrl().getProtocol().equals("hessian"))
            RpcContext.getContext().setAttachments(HessianProtocol.HEADER_MAP_THREAD_LOCAL.get());
        return invoker.invoke(invocation);
    }
}
