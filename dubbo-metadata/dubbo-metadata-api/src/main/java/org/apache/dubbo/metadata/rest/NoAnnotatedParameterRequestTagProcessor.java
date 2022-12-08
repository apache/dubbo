package org.apache.dubbo.metadata.rest;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.metadata.rest.media.MediaType;

import java.lang.reflect.Parameter;

@SPI
public interface NoAnnotatedParameterRequestTagProcessor {
    MediaType consumerContentType();

    String defaultAnnotationClassName();

    void process(Parameter parameter, int parameterIndex, RestMethodMetadata restMethodMetadata);
}
