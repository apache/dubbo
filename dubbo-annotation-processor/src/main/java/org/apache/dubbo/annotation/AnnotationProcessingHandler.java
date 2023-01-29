package org.apache.dubbo.annotation;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Info here.
 */
public interface AnnotationProcessingHandler {

    Set<Class<? extends Annotation>> getAnnotationsToHandle();

    void process(Set<Element> elements,
                 AnnotationProcessorContext annotationProcessorContext);
}
