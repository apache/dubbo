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
package org.apache.dubbo.mcp.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to expose a Dubbo service method as an MCP tool.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface McpTool {

    /**
     * Specifies whether this method should be exposed as an MCP tool.
     * Default is true.
     */
    boolean enabled() default true;

    /**
     * The unique name of the MCP tool. If not specified, the method name will be used.
     */
    String name() default "";

    /**
     * A description of the tool's functionality. This will be visible to AI models.
     * If not specified, a default description will be generated.
     */
    String description() default "";

    /**
     * A list of tags for categorizing the tool.
     */
    String[] tags() default {};

    /**
     * The priority of this tool. Higher values indicate higher priority.
     */
    int priority() default 0;
}
