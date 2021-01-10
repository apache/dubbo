package org.apache.dubbo.metadata.annotation.processing.builder;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import java.util.Map;

@SPI
public interface TypeBuilder<T extends TypeMirror> extends Prioritized {

    /**
     * Test the specified {@link TypeMirror type} is accepted or not
     *
     * @param processingEnv {@link ProcessingEnvironment}
     * @param type          {@link TypeMirror type}
     * @return <code>true</code> if accepted
     */
    boolean accept(ProcessingEnvironment processingEnv, TypeMirror type);

    /**
     * Build the instance of {@link TypeDefinition}
     *
     * @param processingEnv  {@link ProcessingEnvironment}
     * @param type           {@link T type}
     * @return an instance of {@link TypeDefinition}
     */
    TypeDefinition build(ProcessingEnvironment processingEnv, T type, Map<String, TypeDefinition> typeCache);
}
