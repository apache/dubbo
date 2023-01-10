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

public interface JAXRSClassConstants extends RestMetadataConstants.JAX_RS {
    /**
     * The annotation class of @Path
     */
    Class PATH_ANNOTATION_CLASS = resolveClass(PATH_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @FormParam
     */
    Class FORM_PARAM_ANNOTATION_CLASS = resolveClass(FORM_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());

    /**
     * The annotation class of @HeaderParam
     */
    Class HEADER_PARAM_ANNOTATION_CLASS = resolveClass(HEADER_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class of @MatrixParam
     */
    Class MATRIX_PARAM_ANNOTATION_CLASS = resolveClass(MATRIX_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());


    /**
     * The annotation class  of @QueryParam
     */
    Class QUERY_PARAM_ANNOTATION_CLASS = resolveClass(QUERY_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());

    /**
     * The annotation class of @Body
     */
    Class REST_EASY_BODY_ANNOTATION_CLASS = resolveClass(REST_EASY_BODY_ANNOTATION_CLASS_NAME, getClassLoader());

    /**
     * The annotation class of @PathParam
     */
    Class PATH_PARAM_ANNOTATION_CLASS = resolveClass(PATH_PARAM_ANNOTATION_CLASS_NAME, getClassLoader());


}
