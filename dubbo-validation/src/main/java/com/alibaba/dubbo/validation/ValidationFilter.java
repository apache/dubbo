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

import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.groups.Default;

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
    
    private final Validator validator;
    
    public ValidationFilter() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
    
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            Class<?> clazz = invoker.getInterface();
            String methodName = invocation.getMethodName();
            String methodClassName = clazz.getName() + "$" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
            Class<?> methodClass = null;
            try {
                methodClass = Class.forName(methodClassName, false, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
            }
            Set<ConstraintViolation<?>> violations = new HashSet<ConstraintViolation<?>>();
            for (Object arg : invocation.getArguments()) {
                if (methodClass != null) {
                    violations.addAll(validator.validate(arg, Default.class, clazz, methodClass));
                } else {
                    violations.addAll(validator.validate(arg, Default.class, clazz));
                }
            }
            if (violations.size() > 0) {
                throw new ConstraintViolationException(violations);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        return invoker.invoke(invocation);
    }

}
