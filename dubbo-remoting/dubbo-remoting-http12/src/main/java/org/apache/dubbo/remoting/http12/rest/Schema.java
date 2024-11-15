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

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Schema {

    /**
     * Alias for {@link #title()}.
     */
    String value() default "";

    /**
     * The schema group.
     */
    String group() default "";

    /**
     * The type of the schema.
     */
    String type() default "";

    /**
     * The schema's format
     */
    String format() default "";

    /**
     * The name of the schema or property.
     **/
    String name() default "";

    /**
     * A title to explain the purpose of the schema.
     **/
    String title() default "";

    /**
     * The schema's description
     **/
    String description() default "";

    /**
     * The maximum value or length of this schema
     **/
    String max() default "";

    /**
     * The minimum value or length of this schema
     **/
    String min() default "";

    /**
     * The pattern of this schema.
     **/
    String pattern() default "";

    /**
     * An example of this schema.
     **/
    String example() default "";

    /**
     * A class that implements this schema.
     **/
    Class<?> implementation() default Void.class;

    /**
     * A list of allowed schema values
     **/
    String[] enumeration() default {};

    /**
     * Whether this schema is required
     **/
    boolean required() default false;

    /**
     * The default value of this schema
     **/
    String defaultValue() default "";

    /**
     * Whether this schema is read only
     **/
    boolean readOnly() default false;

    /**
     * Whether this schema is written only
     */
    boolean writeOnly() default false;

    /**
     * Whether this schema is nullable
     */
    boolean nullable() default false;

    /**
     * Whether this operation is deprecated
     */
    boolean deprecated() default false;

    /**
     * Whether this schema is hidden.
     */
    boolean hidden() default false;

    /**
     * The extensions of the OpenAPI.
     */
    String[] extensions() default {};
}
