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
 * Annotation for defining OpenAPI on Dubbo service interface.
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;OpenAPI(tags = {"user=User API"}, title = "User Service", description = "User Service API", version = "1.0.0")
 * public interface UserService {
 *     ...
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpenAPI {

    /**
     * The openAPI groups.
     */
    String group() default "";

    /**
     * The openAPI tags.
     * <h5>Supported Syntax</h5>
     * <ul>
     * <li>{@code "name"}</li>
     * <li>{@code "name=description"}</li>
     * </ul>
     * e.g. user=User API
     */
    String[] tags() default {};

    /**
     * The title of the application.
     **/
    String infoTitle() default "";

    /**
     * A short description of the application.
     **/
    String infoDescription() default "";

    /**
     * The version of the API definition.
     **/
    String infoVersion() default "";

    /**
     * A description of the external documentation.
     */
    String docDescription() default "";

    /**
     * The URL of the external documentation.
     */
    String docUrl() default "";

    /**
     * Indicates whether the mapping is hidden in OpenAPI.
     */
    String hidden() default "";

    /**
     * Ordering info.
     */
    int order() default 0;

    /**
     * The extensions of the OpenAPI.
     */
    String[] extensions() default {};
}
