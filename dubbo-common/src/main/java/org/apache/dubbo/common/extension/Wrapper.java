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
package org.apache.dubbo.common.extension;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The annotated class will only work as a wrapper when the condition matches.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Wrapper {

    /**
     * the extension names that need to be wrapped.
     * default is matching when this array is empty.
     */
    String[] matches() default {};

    /**
     * the extension names that need to be excluded.
     */
    String[] mismatches() default {};

    /**
     * absolute ordering, optional
     * ascending order, smaller values will be in the front of the list.
     * @return
     */
    int order() default 0;
}
