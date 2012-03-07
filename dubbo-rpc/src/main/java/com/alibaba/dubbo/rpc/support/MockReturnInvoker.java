/*
 * Copyright 1999-2011 Alibaba Group.
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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.json.JSON;
import com.alibaba.dubbo.common.utils.PojoUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;

/**
 * MockReturnInvoker
 * 
 * @author william.liangf
 * @author chao.liuc
 */
final public class MockReturnInvoker<T> implements Invoker<T> {
    private final URL url ;
    public MockReturnInvoker(URL url) {
        this.url = url;
    }
    
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[0-9]");

    public Result invoke(Invocation invocation) throws RpcException {
    	String mock = getUrl().getParameter(invocation.getMethodName()+"."+Constants.MOCK_KEY);
    	
    	if (StringUtils.isBlank(mock)){
    		mock = getUrl().getParameter(Constants.MOCK_KEY);
    	}
    	if (!StringUtils.isBlank(mock)){
            mock = URL.decode(mock);
            if (mock.startsWith(Constants.FAIL_PREFIX)) {
                mock = mock.substring(Constants.FAIL_PREFIX.length()).trim();
            } else if (mock.startsWith(Constants.FORCE_PREFIX)) {
                mock = mock.substring(Constants.FORCE_PREFIX.length()).trim();
            } else if (mock.startsWith("fail")) {
                mock = mock.substring("fail".length()).trim();
            } else if (mock.startsWith("force")) {
                mock = mock.substring("force".length()).trim();
            }
            if (mock.startsWith(Constants.RETURN_PREFIX)) {
                mock = mock.substring(Constants.RETURN_PREFIX.length()).trim();
            } 
            mock = mock.replace('`', '"');
            if (StringUtils.isBlank(mock)){
            	mock = null;
            }
            try {
                Type[] returnTypes = RpcUtils.getReturnTypes(invocation);
                Object value = parseMockValue(mock, returnTypes);
                return new RpcResult(value);
            } catch (Exception ew) {
            	return getNullResult();
            }
        } else {
        	//没有mock的方法 直接返回null
        	return getNullResult();
        }
    }
    
    private Result getNullResult(){
    	RpcResult result = new RpcResult();
    	result.setValue(null);
    	return result;
    }
    public static Object parseMockValue(String mock) throws Exception {
        return parseMockValue(mock, null);
    }
    
    public static Object parseMockValue(String mock, Type[] returnTypes) throws Exception {
        Object value = null;
        if ("empty".equals(mock)) {
            value = ReflectUtils.getEmptyObject(returnTypes != null && returnTypes.length > 0 ? (Class<?>)returnTypes[0] : null);
        } else if ("null".equals(mock)) {
            value = null;
        } else if ("true".equals(mock)) {
            value = true;
        } else if ("false".equals(mock)) {
            value = false;
        } else if (mock.length() >=2 && (mock.startsWith("\"") && mock.endsWith("\"")
                || mock.startsWith("\'") && mock.endsWith("\'"))) {
            value = mock.subSequence(1, mock.length() - 1);
        } else if (NUMBER_PATTERN.matcher(mock).matches()) {
            value = JSON.parse(mock);
        } else if (mock.startsWith("{")) {
            value = JSON.parse(mock, Map.class);
        } else if (mock.startsWith("[")) {
            value = JSON.parse(mock, List.class);
        } else {
            value = mock;
        }
        if (returnTypes != null && returnTypes.length > 0) {
            value = PojoUtils.realize(value, (Class<?>)returnTypes[0], returnTypes.length > 1 ? returnTypes[1] : null);
        }
        return value;
    }

	public URL getUrl() {
		return this.url;
	}

	public boolean isAvailable() {
		return true;
	}

	public void destroy() {
		//do nothing
	}

	public Class<T> getInterface() {
		return null;
	}
}