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

import java.lang.annotation.Annotation;

import static org.apache.dubbo.common.utils.ClassUtils.getClassLoader;
import static org.apache.dubbo.common.utils.ClassUtils.resolveClass;

/**
 * The REST Metadata Constants definition interface
 *
 * @since 2.7.6
 */
public interface RestMetadataConstants {

    /**
     * The encoding of metadata
     */
    String METADATA_ENCODING = "UTF-8";

    /**
     * {@link ServiceRestMetadata} Resource PATH
     */
    String SERVICE_REST_METADATA_RESOURCE_PATH = "META-INF/dubbo/service-rest-metadata.json";

    /**
     * JAX-RS
     */
    interface JAX_RS {

        /**
         * The annotation class name of @Path
         */
        String PATH_ANNOTATION_CLASS_NAME = "javax.ws.rs.Path";

        /**
         * The annotation class name of @HttpMethod
         */
        String HTTP_METHOD_ANNOTATION_CLASS_NAME = "javax.ws.rs.HttpMethod";

        /**
         * The annotation class name of @Produces
         */
        String PRODUCES_ANNOTATION_CLASS_NAME = "javax.ws.rs.Produces";

        /**
         * The annotation class name of @Consumes
         */
        String CONSUMES_ANNOTATION_CLASS_NAME = "javax.ws.rs.Consumes";

        /**
         * The annotation class name of @DefaultValue
         */
        String DEFAULT_VALUE_ANNOTATION_CLASS_NAME = "javax.ws.rs.DefaultValue";

        /**
         * The annotation class name of @FormParam
         */
        String FORM_PARAM_ANNOTATION_CLASS_NAME = "javax.ws.rs.FormParam";

        /**
         * The annotation class name of @HeaderParam
         */
        String HEADER_PARAM_ANNOTATION_CLASS_NAME = "javax.ws.rs.HeaderParam";

        /**
         * The annotation class name of @MatrixParam
         */
        String MATRIX_PARAM_ANNOTATION_CLASS_NAME = "javax.ws.rs.MatrixParam";

        /**
         * The annotation class name of @QueryParam
         */
        String QUERY_PARAM_ANNOTATION_CLASS_NAME = "javax.ws.rs.QueryParam";
    }

    /**
     * Spring MVC
     */
    interface SPRING_MVC {

        /**
         * The annotation class name of @Controller
         */
        String CONTROLLER_ANNOTATION_CLASS_NAME = "org.springframework.stereotype.Controller";

        /**
         * The annotation class name of @RequestMapping
         */
        String REQUEST_MAPPING_ANNOTATION_CLASS_NAME = "org.springframework.web.bind.annotation.RequestMapping";

        /**
         * The annotation class name of @RequestHeader
         */
        String REQUEST_HEADER_ANNOTATION_CLASS_NAME = "org.springframework.web.bind.annotation.RequestHeader";

        /**
         * The annotation class name of @RequestParam
         */
        String REQUEST_PARAM_ANNOTATION_CLASS_NAME = "org.springframework.web.bind.annotation.RequestParam";

        /**
         * The class of @Controller
         *
         * @since 2.7.9
         */
        Class<? extends Annotation> CONTROLLER_ANNOTATION_CLASS = (Class<? extends Annotation>) resolveClass(CONTROLLER_ANNOTATION_CLASS_NAME, getClassLoader());

        /**
         * The class of @RequestMapping
         *
         * @since 2.7.9
         */
        Class<? extends Annotation> REQUEST_MAPPING_ANNOTATION_CLASS = (Class<? extends Annotation>) resolveClass(REQUEST_MAPPING_ANNOTATION_CLASS_NAME, getClassLoader());

        /**
         * The annotation class name of AnnotatedElementUtils
         *
         * @since 2.7.9
         */
        String ANNOTATED_ELEMENT_UTILS_CLASS_NAME = "org.springframework.core.annotation.AnnotatedElementUtils";

        /**
         * The class of AnnotatedElementUtils
         *
         * @since 2.7.9
         */
        Class<?> ANNOTATED_ELEMENT_UTILS_CLASS = resolveClass(ANNOTATED_ELEMENT_UTILS_CLASS_NAME, getClassLoader());
    }
}
