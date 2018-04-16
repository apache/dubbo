/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.validation.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.validation.Validation;
import com.alibaba.dubbo.validation.Validator;

/**
 * ValidationFilter
 *
 * 参数验证过滤器实现类
 */
@Activate(group = {Constants.CONSUMER, Constants.PROVIDER}, value = Constants.VALIDATION_KEY, order = 10000)
public class ValidationFilter implements Filter {

    /**
     * Validation$Adaptive 对象
     *
     * 通过 Dubbo SPI 机制，调用 {@link #setValidation(Validation)} 方法，进行注入
     */
    private Validation validation;

    public void setValidation(Validation validation) {
        this.validation = validation;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (validation != null && !invocation.getMethodName().startsWith("$") // 非泛化调用和回音调用等方法
                && ConfigUtils.isNotEmpty(invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.VALIDATION_KEY))) { // 方法开启 Cache 功能
            try {
                // 获得 Validator 对象
                Validator validator = validation.getValidator(invoker.getUrl());
                // 使用 Validator ，验证方法参数。若不合法，抛出异常。
                if (validator != null) {
                    validator.validate(invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments());
                }
            } catch (RpcException e) {
                throw e;
            } catch (Throwable t) {
                return new RpcResult(t);
            }
        }
        // 服务调用
        return invoker.invoke(invocation);
    }

}
