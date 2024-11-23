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
package org.apache.dubbo.remoting.http12.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for defining parameters on Dubbo service method parameters.
 * Provides metadata such as parameter type, default value, and whether the parameter is required.
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;Param(value = "x-version", type = ParamType.Header, required = false) String version
 * </pre>
 * @see <a href="https://dubbo-next.staged.apache.org/zh-cn/overview/mannual/java-sdk/reference-manual/protocol/tripe-rest-manual/#kmCzf">Tripe Rest Manual</a>
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    String DEFAULT_NONE = "\n\t\t\n\t\t\n_DEFAULT_NONE_\n\t\t\t\t\n";

    /**
     * The name of the parameter.
     * If not specified, the parameter name is derived from the method signature or field name.
     */
    String value() default "";

    /**
     * The type of the parameter, such as query, header, or path variable.
     * Defaults to {@link ParamType#Param}.
     */
    ParamType type() default ParamType.Param;

    /**
     * Indicates whether the parameter is required.
     * Defaults to {@code true}. If set to {@code false}, the parameter is optional.
     */
    boolean required() default true;

    /**
     * Specifies a default value for the parameter.
     * Defaults to {@link #DEFAULT_NONE}, indicating that there is no default value.
     */
    String defaultValue() default DEFAULT_NONE;
}
