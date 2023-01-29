package org.apache.dubbo.annotation;

import org.apache.dubbo.annotation.handler.DeprecatedHandler;
import org.apache.dubbo.annotation.permit.Permit;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Info here.
 *
 * @author Andy Cheung
 */
@SupportedAnnotationTypes("*")
public class DispatchingAnnotationProcessor extends AbstractProcessor {

    private static final Set<Class<? extends AnnotationProcessingHandler>> handlerClasses = new HashSet<>(
        Arrays.asList(DeprecatedHandler.class)
    );

    private static final Set<AnnotationProcessingHandler> handlers = loadHandlers();

    private AnnotationProcessorContext apContext;

    private static Set<AnnotationProcessingHandler> loadHandlers() {
        return Collections.unmodifiableSet(handlerClasses.stream().map(x -> {
            try {
                return x.getConstructor().newInstance();
            } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet()));
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        Permit.addOpens();
        super.init(processingEnv);

        apContext = AnnotationProcessorContext.fromProcessingEnvironment(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (AnnotationProcessingHandler i : handlers) {
            Set<Element> elements = new HashSet<>(16);

            for (Class<? extends Annotation> annotationClass : i.getAnnotationsToHandle()) {
                elements.addAll(roundEnv.getElementsAnnotatedWith(annotationClass));
            }

            i.process(elements, apContext);
        }

        return true;
    }
}
