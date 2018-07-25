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
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

/**
 * {@link RestService @RestService} {@link HandlerMethodReturnValueHandler} implementation
 *
 * @since 2.7.0
 */
public class RestServiceHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

    private final RequestResponseBodyMethodProcessor processor;

    public RestServiceHandlerMethodReturnValueHandler(RequestResponseBodyMethodProcessor processor) {
        this.processor = processor;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        Class<?> serviceClass = returnType.getDeclaringClass();
        return AnnotatedElementUtils.hasAnnotation(serviceClass, RestService.class);
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest) throws Exception {

        processor.handleReturnValue(returnValue, returnType, mavContainer, webRequest);

    }

}
