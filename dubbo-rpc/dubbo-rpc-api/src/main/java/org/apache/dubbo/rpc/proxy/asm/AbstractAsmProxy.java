package org.apache.dubbo.rpc.proxy.asm;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;

public abstract class AbstractAsmProxy {

	private static final Logger logger = LoggerFactory.getLogger(AbstractAsmProxy.class);
    private final Invoker<?> invoker;

    public AbstractAsmProxy(Invoker<?> handler) {
        this.invoker = handler;
    }

	
    public Object invoke(MethodStatement ms) throws Throwable {
		return invoke(ms , null);
	}
    
	public Object invoke(MethodStatement ms, Object[] args) throws Throwable {
		return invoker.invoke(createInvocation(ms , args)).recreate();
	}
	
	private RpcInvocation createInvocation(MethodStatement ms, Object[] args) {
        RpcInvocation invocation = new RpcInvocation(ms.getMethod(),null, args);
        if (ms.isFutureReturnType()) {
            invocation.setAttachment(Constants.FUTURE_RETURNTYPE_KEY, "true");
            invocation.setAttachment(Constants.ASYNC_KEY, "true");
        }
        return invocation;
    }
}
