package org.apache.dubbo.metadata.rest;

import org.apache.dubbo.metadata.rest.media.MediaType;

import java.lang.reflect.Parameter;
import java.util.Set;

import static org.apache.dubbo.common.utils.ClassUtils.*;

public abstract class AbstractNoAnnotatedParameterProcessor implements NoAnnotatedParameterRequestTagProcessor {

    public void process(Parameter parameter, int parameterIndex, RestMethodMetadata restMethodMetadata) {
        MediaType mediaType = consumerContentType();
        if (!contentTypeSupport(restMethodMetadata, mediaType, parameter.getType())) {
            return;
        }
        boolean isFormBody = isFormContentType(restMethodMetadata);
        addArgInfo(parameter, parameterIndex, restMethodMetadata, isFormBody);
    }

    private boolean contentTypeSupport(RestMethodMetadata restMethodMetadata, MediaType mediaType, Class paramType) {

        // @RequestParam String,number param
        if (mediaType.equals(MediaType.ALL_VALUE) && (String.class == paramType || Number.class.isAssignableFrom(paramType))) {
            return true;
        }

        Set<String> consumes = restMethodMetadata.getRequest().getConsumes();
        for (String consume : consumes) {
            if (consume.contains(mediaType.value)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isFormContentType(RestMethodMetadata restMethodMetadata) {

        return false;
    }


    protected void addArgInfo(Parameter parameter, int parameterIndex,
                              RestMethodMetadata restMethodMetadata, boolean isFormBody) {
        ArgInfo argInfo = ArgInfo.build(parameterIndex, parameter)
            .setParamAnnotationType(resolveClass(defaultAnnotationClassName(), getClassLoader()))
            .setAnnotationNameAttribute(parameter.getName()).setFormContentType(isFormBody);
        restMethodMetadata.addArgInfo(argInfo);
    }
}
