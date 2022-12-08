package org.apache.dubbo.metadata.rest.springmvc;

import org.apache.dubbo.metadata.rest.AbstractNoAnnotatedParameterProcessor;
import org.apache.dubbo.metadata.rest.media.MediaType;

import static org.apache.dubbo.metadata.rest.RestMetadataConstants.SPRING_MVC.REQUEST_PARAM_ANNOTATION_CLASS_NAME;
import static org.apache.dubbo.metadata.rest.media.MediaType.ALL_VALUE;

public class NoRequestParamProcessor extends AbstractNoAnnotatedParameterProcessor {
    @Override
    public MediaType consumerContentType() {
        return ALL_VALUE;
    }

    @Override
    public String defaultAnnotationClassName() {
        return REQUEST_PARAM_ANNOTATION_CLASS_NAME;
    }
}
