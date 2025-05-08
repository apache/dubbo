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
 * Annotation for defining an operation in the OpenAPI specification for Dubbo services.
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;Operation(method = "GET", summary = "Retrieve user", tags = {"user", "retrieve"})
 * public User getUser(String id) {
 *     ...
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Operation {

    /**
     * Alias for {@link #summary()}.
     */
    String value() default "";

    /**
     * The operation group.
     */
    String group() default "";

    /**
     * The operation version.
     */
    String version() default "";

    /**
     * The HTTP method for this operation.
     */
    String method() default "";

    /**
     * The operation tags.
     */
    String[] tags() default {};

    /**
     * The ID of this operation.
     **/
    String id() default "";

    /**
     * A brief description of this operation. Should be 120 characters or fewer.
     */
    String summary() default "";

    /**
     * A verbose description of the operation.
     */
    String description() default "";

    /**
     * Whether this operation is deprecated
     */
    boolean deprecated() default false;

    /**
     * Indicates whether the operation is hidden in OpenAPI.
     */
    String hidden() default "";

    /**
     * The extensions of the OpenAPI.
     */
    String[] extensions() default {};
}
