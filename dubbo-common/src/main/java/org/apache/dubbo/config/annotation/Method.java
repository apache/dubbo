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
package org.apache.dubbo.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @since 2.6.5
 *  *
 *  * 2018/9/29
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
@Inherited
public @interface Method {
    String name();

    int timeout() default -1;

    int retries() default -1;

    String loadbalance() default "";

    boolean async() default false;

    boolean sent() default true;

    int actives() default 0;

    int executes() default 0;

    boolean deprecated() default false;

    boolean sticky() default false;

    boolean isReturn() default true;

    String oninvoke() default "";

    String onreturn() default "";

    String onthrow() default "";

    String cache() default "";

    String validation() default "";

    String merger() default "";

    Argument[] arguments() default {};
}
