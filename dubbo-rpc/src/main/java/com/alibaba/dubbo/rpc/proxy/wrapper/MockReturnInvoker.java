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
package com.alibaba.dubbo.rpc.proxy.wrapper;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.json.JSON;
import com.alibaba.dubbo.common.utils.PojoUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.support.DelegateInvoker;
import com.alibaba.dubbo.rpc.support.RpcUtils;

/**
 * MockReturnInvoker
 * 
 * @author william.liangf
 */
public class MockReturnInvoker<T> extends DelegateInvoker<T> {
    
    public MockReturnInvoker(Invoker<T> invoker) {
        super(invoker);
    }
    
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[0-9]");

    public Result invoke(Invocation invocation) throws RpcException {
        try {
            return super.invoke(invocation);
        } catch (RpcException e) {
            String mock = getUrl().getMethodParameter(invocation.getMethodName(), Constants.MOCK_KEY);
            if (mock != null && mock.length() > 0) {
                mock = URL.decode(mock);
                if (mock.startsWith(Constants.RETURN_PREFIX)) {
                    mock = mock.substring(Constants.RETURN_PREFIX.length()).trim();
                }
                mock = mock.replace('`', '"');
                try {
                    Type[] returnTypes = RpcUtils.getReturnTypes(invocation);
                    Object value = parseMockValue(mock, returnTypes);
                    return new RpcResult(value);
                } catch (Exception ew) {
                }
            }
            throw e;
        }
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

}