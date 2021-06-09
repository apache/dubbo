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
package org.apache.dubbo.config.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parameter
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Parameter {

    /**
     * Specify the parameter key when append parameters to url
     */
    String key() default "";

    /**
     * If required=true, the value must not be empty when append to url
     */
    boolean required() default false;

    /**
     * If excluded=true, ignore it when append parameters to url
     */
    boolean excluded() default false;

    /**
     * if escaped=true, escape it when append parameters to url
     */
    boolean escaped() default false;

    /**
     * If attribute=false, ignore it when processing refresh()/getMetadata()/equals()/toString()
     */
    boolean attribute() default true;

    /**
     * If append=true, append new value to exist value instead of overriding it.
     */
    boolean append() default false;

}
