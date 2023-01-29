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

import org.apache.dubbo.metadata.MetadataConstants;

import java.util.Arrays;
import java.util.List;

public enum ParamType {
    HEADER(Arrays.asList(JAXRSClassConstants.HEADER_PARAM_ANNOTATION_CLASS,
        SpringMvcClassConstants.REQUEST_HEADER_ANNOTATION_CLASS)),

    PARAM(Arrays.asList(JAXRSClassConstants.QUERY_PARAM_ANNOTATION_CLASS,
        SpringMvcClassConstants.REQUEST_PARAM_ANNOTATION_CLASS)),

    BODY(Arrays.asList(
        JAXRSClassConstants.REST_EASY_BODY_ANNOTATION_CLASS,
        SpringMvcClassConstants.REQUEST_BODY_ANNOTATION_CLASS)),
    // TODO how to match arg type ?
    REQ_OR_RES(Arrays.asList(MetadataConstants.JAKARTA_SERVLET_REQ_CLASS,
        MetadataConstants.JAKARTA_SERVLET_RES_CLASS,
        MetadataConstants.JAVAX_SERVLET_REQ_CLASS,
        MetadataConstants.JAKARTA_SERVLET_RES_CLASS)),

    PATH(Arrays.asList(JAXRSClassConstants.PATH_PARAM_ANNOTATION_CLASS,
        SpringMvcClassConstants.PATH_VARIABLE_ANNOTATION_CLASS)),

    FORM(Arrays.asList(JAXRSClassConstants.FORM_PARAM_ANNOTATION_CLASS)),

    EMPTY(Arrays.asList());
    private List<Class> annotationClasses;


    ParamType(List<Class> annotationClasses) {
        this.annotationClasses = annotationClasses;
    }


    public boolean supportAnno(Class anno) {
        return this.annotationClasses.contains(anno);
    }

    public boolean isReqOrRes(Class clazz) {
        for (Class annotationClass : annotationClasses) {
            if (annotationClass.isAssignableFrom(clazz)) {
                return true;
            }
        }

        return false;
    }

}
