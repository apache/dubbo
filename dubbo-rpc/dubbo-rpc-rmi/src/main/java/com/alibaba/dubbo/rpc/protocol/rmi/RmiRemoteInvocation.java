/*
 * Copyright 2013-2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.rmi;

import com.alibaba.dubbo.rpc.RpcContext;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.support.RemoteInvocation;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:takeseem@gmail.com">杨浩</a>
 */
public class RmiRemoteInvocation extends RemoteInvocation {
    private static final long serialVersionUID = 1L;
    private static final String dubboAttachmentsAttrName = "dubbo.attachments";

    /**
     * 构造将在消费端执行
     */
    public RmiRemoteInvocation(MethodInvocation methodInvocation) {
        super(methodInvocation);
        addAttribute(dubboAttachmentsAttrName, new HashMap<String, String>(RpcContext.getContext().getAttachments()));
    }

    /**
     * 服务端执行时，重新放入上下文（虽然这时上下文在ContextFilter执行时将被Invocation的attachments覆盖，我们在Invocation构造时还原attachments, see InvokerInvocationHandler）
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object targetObject) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        RpcContext context = RpcContext.getContext();
        context.setAttachments((Map<String, String>) getAttribute(dubboAttachmentsAttrName));
        try {
            return super.invoke(targetObject);
        } finally {
            context.setAttachments(null);
        }
    }
}