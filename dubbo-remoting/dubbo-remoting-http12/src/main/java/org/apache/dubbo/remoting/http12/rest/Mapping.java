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

import org.apache.dubbo.remoting.http12.HttpMethods;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for mapping Dubbo services to Rest endpoints.
 *
 * <p>
 * Example usage:
 * <pre>
 * &#64;Mapping(value = "/example", method = HttpMethods.GET, produces = "application/json")
 * String handleExample();
 * </pre>
 * @see <a href="https://dubbo-next.staged.apache.org/zh-cn/overview/mannual/java-sdk/reference-manual/protocol/tripe-rest-manual/#Q6XyG">Tripe Rest Manual</a>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Mapping {

    /**
     * Alias for {@link #path()}.
     * The path patterns to be mapped.
     * If not specified, the method or class name is used as the default.
     */
    String[] value() default {};

    /**
     * Specifies the path patterns to be mapped.
     * If not specified, the method or class name is used as the default.
     */
    String[] path() default {};

    /**
     * Defines the HTTP methods supported by the mapped handler.
     * Supports values like GET, POST, etc.
     */
    HttpMethods[] method() default {};

    /**
     * Specifies the request parameters that must be present for this mapping to be invoked.
     * Example: "param1=value1", "param2".
     */
    String[] params() default {};

    /**
     * Specifies the request headers that must be present for this mapping to be invoked.
     * Example: "content-type=application/json".
     */
    String[] headers() default {};

    /**
     * Specifies the media types that the mapped handler consumes.
     * Example: "application/json", "text/plain".
     */
    String[] consumes() default {};

    /**
     * Specifies the media types that the mapped handler produces.
     * Example: "application/json", "text/html".
     */
    String[] produces() default {};

    /**
     * Indicates whether the mapping is enabled.
     * Defaults to {@code true}. If set to {@code false}, the mapping will be ignored.
     */
    boolean enabled() default true;
}
