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

package org.apache.dubbo.spring.security.utils;

final public class SecurityNames {

    public static final String SECURITY_AUTHENTICATION_CONTEXT_KEY = "security_authentication_context";

    public static final String SECURITY_CONTEXT_HOLDER_CLASS_NAME = "org.springframework.security.core.context.SecurityContextHolder";
    public static final String CORE_JACKSON_2_MODULE_CLASS_NAME = "org.springframework.security.jackson2.CoreJackson2Module";
    public static final String OBJECT_MAPPER_CLASS_NAME = "com.fasterxml.jackson.databind.ObjectMapper";
    public static final String JAVA_TIME_MODULE_CLASS_NAME = "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule";
    public static final String SIMPLE_MODULE_CLASS_NAME = "com.fasterxml.jackson.databind.module.SimpleModule";

    private SecurityNames() {}

}
