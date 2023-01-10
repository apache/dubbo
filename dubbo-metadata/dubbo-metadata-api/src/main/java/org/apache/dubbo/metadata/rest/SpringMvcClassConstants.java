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
package org.apache.dubbo.metadata.rest;

import static org.apache.dubbo.common.utils.ClassUtils.getClassLoader;
import static org.apache.dubbo.common.utils.ClassUtils.resolveClass;

public interface SpringMvcClassConstants extends RestMetadataConstants.SPRING_MVC {
    /**
     * The annotation class of @RequestMapping
     */
    Class REQUEST_MAPPING_ANNOTATION_CLASS = resolveClass(REQUEST_MAPPING_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @RequestHeader
     */
    Class REQUEST_HEADER_ANNOTATION_CLASS = resolveClass(REQUEST_HEADER_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @RequestParam
     */
    Class REQUEST_PARAM_ANNOTATION_CLASS = resolveClass(REQUEST_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @RequestBody
     */
    Class REQUEST_BODY_ANNOTATION_CLASS = resolveClass(REQUEST_BODY_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @RequestBody
     */
    Class  PATH_VARIABLE_ANNOTATION_CLASS= resolveClass(PATH_VARIABLE_ANNOTATION_CLASS_NAME, getClassLoader());
}
