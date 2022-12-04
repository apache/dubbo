package org.apache.dubbo.rpc.protocol.rest.annotation;

import java.util.Arrays;
import java.util.List;

public enum ParamType {
    HEADER(Arrays.asList()),
    PARAM(Arrays.asList()),
    BODY(Arrays.asList()),
    REQ_OR_RES(Arrays.asList()),
    PATH(Arrays.asList());

    private List<Class> annotationClasses;


    ParamType(List<Class> annotationClasses) {
        this.annotationClasses = annotationClasses;
    }


    public boolean supportAnno(Class anno) {
        return this.annotationClasses.contains(anno);
    }

}
