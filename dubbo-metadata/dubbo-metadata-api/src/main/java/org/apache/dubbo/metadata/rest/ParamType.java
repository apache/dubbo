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

import org.apache.dubbo.metadata.rest.tag.BodyTag;
import org.apache.dubbo.metadata.rest.tag.ParamTag;

import java.util.ArrayList;
import java.util.List;

public enum ParamType {
    HEADER(addSupportTypes(JAXRSClassConstants.HEADER_PARAM_ANNOTATION_CLASS,
        SpringMvcClassConstants.REQUEST_HEADER_ANNOTATION_CLASS)),

    PARAM(addSupportTypes(JAXRSClassConstants.QUERY_PARAM_ANNOTATION_CLASS,
        SpringMvcClassConstants.REQUEST_PARAM_ANNOTATION_CLASS, ParamTag.class)),

    BODY(addSupportTypes(
        JAXRSClassConstants.REST_EASY_BODY_ANNOTATION_CLASS,
        SpringMvcClassConstants.REQUEST_BODY_ANNOTATION_CLASS, BodyTag.class)),

    PATH(addSupportTypes(JAXRSClassConstants.PATH_PARAM_ANNOTATION_CLASS,
        SpringMvcClassConstants.PATH_VARIABLE_ANNOTATION_CLASS)),

    FORM(addSupportTypes(JAXRSClassConstants.FORM_PARAM_ANNOTATION_CLASS,
        SpringMvcClassConstants.REQUEST_BODY_ANNOTATION_CLASS)),

    PROVIDER_BODY(addSupportTypes(
        JAXRSClassConstants.REST_EASY_BODY_ANNOTATION_CLASS,JAXRSClassConstants.FORM_PARAM_ANNOTATION_CLASS,
        SpringMvcClassConstants.REQUEST_BODY_ANNOTATION_CLASS, BodyTag.class)),

    EMPTY(addSupportTypes());
    private List<Class> annotationClasses;


    ParamType(List<Class> annotationClasses) {
        this.annotationClasses = annotationClasses;
    }


    public boolean supportAnno(Class anno) {
        if (anno == null) {
            return false;
        }
        return this.annotationClasses.contains(anno);
    }

    /**
     * exclude null types
     *
     * @param classes
     * @return
     */
    private static List<Class> addSupportTypes(Class... classes) {

        ArrayList<Class> types = new ArrayList<>();

        for (Class clazz : classes) {

            if (clazz == null) {
                continue;
            }

            types.add(clazz);
        }

        return types;


    }


}
