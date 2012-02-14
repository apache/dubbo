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
package com.alibaba.dubbo.validation;

import java.lang.reflect.Method;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * ValidationFilter
 * 
 * @author william.liangf
 */
public class ValidationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ValidationFilter.class);
    
    private final ValidatorFactory factory;

    private final Validator validator;
    
    public ValidationFilter() {
        factory = Validation.buildDefaultValidatorFactory(); 
        validator = factory.getValidator();
    }
    
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            Class<?> cls = invoker.getInterface();
            Method method = cls.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
            for (Object arg : invocation.getArguments()) {
                // TODO
                validator.validate(arg);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        return invoker.invoke(invocation);
    }

}
