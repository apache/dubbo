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
package com.alibaba.dubbo.rpc.protocol.spring.webmvc.method.annotation;

import com.alibaba.dubbo.rpc.protocol.spring.webmvc.annotation.RestService;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link RestService @RestService} {@link HandlerAdapter}
 *
 * @since 2.7.0
 */
public class RestServiceHandlerAdapter extends RequestMappingHandlerAdapter {

    protected boolean supportsInternal(HandlerMethod handlerMethod) {
        Class<?> beanType = handlerMethod.getBeanType();
        return AnnotatedElementUtils.hasAnnotation(beanType, RestService.class);
    }

    @Override
    public void afterPropertiesSet() {

        super.afterPropertiesSet();

        final List<HandlerMethodReturnValueHandler> allReturnValueHandlers = getReturnValueHandlers();

        final List<HandlerMethodReturnValueHandler> restReturnValueHandlers = new LinkedList<HandlerMethodReturnValueHandler>();

        for (HandlerMethodReturnValueHandler returnValueHandler : allReturnValueHandlers) {

            if (ClassUtils.isAssignableValue(RequestResponseBodyMethodProcessor.class, returnValueHandler)) {

                RequestResponseBodyMethodProcessor delegate = (RequestResponseBodyMethodProcessor) returnValueHandler;
                HandlerMethodReturnValueHandler restReturnValueHandler = new RestServiceHandlerMethodReturnValueHandler(delegate);
                restReturnValueHandlers.add(restReturnValueHandler);
            }

        }

        setReturnValueHandlers(restReturnValueHandlers);

    }

}
