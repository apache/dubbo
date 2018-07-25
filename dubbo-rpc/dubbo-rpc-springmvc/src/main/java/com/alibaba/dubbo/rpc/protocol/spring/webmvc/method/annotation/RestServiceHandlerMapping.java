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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@link RestService @RestService} {@link HandlerMapping} implementation that is compatible with
 * Spring Web MVC standard {@link RequestMapping @RequestMapping} annotation.
 *
 * @see RequestMapping
 * @see RequestMappingHandlerMapping
 * @since 2.7.0
 */
public class RestServiceHandlerMapping extends RequestMappingHandlerMapping {

    /**
     * Default Constructor with highest precedence order.
     */
    public RestServiceHandlerMapping() {
        setOrder(HIGHEST_PRECEDENCE);
    }

    @Override
    protected boolean isHandler(Class beanType) {
        return AnnotatedElementUtils.hasAnnotation(beanType, RestService.class);
    }

}
