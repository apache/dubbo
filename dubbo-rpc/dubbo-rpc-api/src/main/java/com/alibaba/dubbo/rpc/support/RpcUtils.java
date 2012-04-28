/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.support;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcInvocation;

/**
 * RpcUtils
 * 
 * @author william.liangf
 * @author chao.liuc
 */
public class RpcUtils {

    private static final Logger logger = LoggerFactory.getLogger(RpcUtils.class);

    public static Class<?> getReturnType(Invocation invocation) {
        try {
            if (invocation != null && invocation.getInvoker() != null
                    && invocation.getInvoker().getUrl() != null
                    && ! invocation.getMethodName().startsWith("$")) {
                String service = invocation.getInvoker().getUrl().getServiceInterface();
                if (service != null && service.length() > 0) {
                    Class<?> cls = ReflectUtils.forName(service);
                    Method method = cls.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
                    if (method.getReturnType() == void.class) {
                        return null;
                    }
                    return method.getReturnType();
                }
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        return null;
    }

    public static Type[] getReturnTypes(Invocation invocation) {
        try {
            if (invocation != null && invocation.getInvoker() != null
                    && invocation.getInvoker().getUrl() != null
                    && ! invocation.getMethodName().startsWith("$")) {
                String service = invocation.getInvoker().getUrl().getServiceInterface();
                if (service != null && service.length() > 0) {
                    Class<?> cls = ReflectUtils.forName(service);
                    Method method = cls.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
                    if (method.getReturnType() == void.class) {
                        return null;
                    }
                    return new Type[]{method.getReturnType(), method.getGenericReturnType()};
                }
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        return null;
    }
    
    private static final AtomicLong INVOKE_ID = new AtomicLong(0);
    
	public static Long getInvocationId(Invocation inv) {
    	String id = inv.getAttachment(Constants.Attachments.INVOCATIONID_KEY);
		return id == null ? null : new Long(id);
	}
    
    /**
     * 幂等操作:异步操作默认添加invocation id
     * @param url
     * @param inv
     */
    public static void attachInvocationIdIfAsync(URL url, Invocation inv){
    	if (isAttachInvocationId(url, inv) && getInvocationId(inv) == null && inv instanceof RpcInvocation) {
    		((RpcInvocation)inv).setAttachment(Constants.Attachments.INVOCATIONID_KEY, String.valueOf(INVOKE_ID.getAndIncrement()));
        }
    }
    
    private static boolean isAttachInvocationId(URL url , Invocation invocation) {
    	String value = url.getMethodParameter(invocation.getMethodName(), Constants.AUTO_ATTACH_INVOCATIONID_KEY);
    	if ( value == null ) {
    		//异步操作默认添加invocationid
    		return url.getMethodParameter(invocation.getMethodName(), Constants.ASYNC_KEY, false);
    	} else if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
    		//设置为添加，则一定添加
    		return true;
    	} else {
    		//value为false时，不添加
    		return false;
    	}
    }
    
    public static String getMethodName(Invocation invocation){
    	String methodName; 
    	if(Constants.$INVOKE.equals(invocation.getMethodName()) 
                && invocation.getArguments() != null 
                && invocation.getArguments().length >0 
                && invocation.getArguments()[0] != null){
            //the frist argument must be real method name;
            methodName = invocation.getArguments()[0].toString();
        }else {
            methodName = invocation.getMethodName();
        }
    	return methodName;
    }
    
    public static boolean isAsync(URL url, Invocation inv) {
    	boolean isAsync ;
    	//如果Java代码中设置优先.
    	if (RpcContext.getContext().isAsync() == null ) {
    		isAsync = url.getMethodParameter(getMethodName(inv), Constants.ASYNC_KEY, false);
    	} else {
    		isAsync = RpcContext.getContext().isAsync();
    	}
    	return isAsync;
    }
    
}
