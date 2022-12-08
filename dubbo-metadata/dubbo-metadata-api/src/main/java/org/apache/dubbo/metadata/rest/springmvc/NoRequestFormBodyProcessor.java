package org.apache.dubbo.metadata.rest.springmvc;

import org.apache.dubbo.metadata.rest.AbstractNoAnnotatedParameterProcessor;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.media.MediaType;

import static org.apache.dubbo.metadata.rest.RestMetadataConstants.SPRING_MVC.REQUEST_BODY_ANNOTATION_CLASS_NAME;
import static org.apache.dubbo.metadata.rest.media.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

public class NoRequestFormBodyProcessor extends AbstractNoAnnotatedParameterProcessor {
    @Override
    public MediaType consumerContentType() {
        return APPLICATION_FORM_URLENCODED_VALUE;
    }

    @Override
    public String defaultAnnotationClassName() {
        return REQUEST_BODY_ANNOTATION_CLASS_NAME;
    }

    @Override
    protected boolean isFormContentType(RestMethodMetadata restMethodMetadata) {
        return true;
    }
}
