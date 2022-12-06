package org.apache.dubbo.rpc.protocol.mvc.annotation;

import org.apache.dubbo.metadata.rest.JAX_RSClassConstants;
import org.apache.dubbo.metadata.rest.SPRING_MVCClassConstants;
import org.apache.dubbo.rpc.protocol.mvc.constans.RestConstant;

import java.util.Arrays;
import java.util.List;

public enum ParamType {
    HEADER(Arrays.asList(JAX_RSClassConstants.HEADER_PARAM_ANNOTATION_CLASS, SPRING_MVCClassConstants.REQUEST_HEADER_ANNOTATION_CLASS)),
    PARAM(Arrays.asList(JAX_RSClassConstants.QUERY_PARAM_ANNOTATION_CLASS, SPRING_MVCClassConstants.REQUEST_PARAM_ANNOTATION_CLASS)),
    BODY(Arrays.asList(JAX_RSClassConstants.REST_EASY_BODY_ANNOTATION_CLASS, SPRING_MVCClassConstants.REQUEST_BODY_ANNOTATION_CLASS)),
    // TODO how to match arg type ?
    REQ_OR_RES(Arrays.asList(RestConstant.JAKARTA_SERVLET_REQ_CLASS,
        RestConstant.JAKARTA_SERVLET_RES_CLASS,
        RestConstant.JAVAX_SERVLET_REQ_CLASS,
        RestConstant.JAKARTA_SERVLET_RES_CLASS)),
    PATH(Arrays.asList(JAX_RSClassConstants.PATH_ANNOTATION_CLASS, SPRING_MVCClassConstants.PATH_VARIABLE_ANNOTATION_CLASS)),
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
