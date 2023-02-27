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
package org.apache.dubbo.metadata.annotation.processing;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.apache.dubbo.metadata.annotation.processing.util.ServiceAnnotationUtils.SUPPORTED_ANNOTATION_TYPES;

/**
 * Abstract {@link Processor} for the classes that were annotated by Dubbo's @Service
 *
 * @since 2.7.6
 */
public abstract class AbstractServiceAnnotationProcessor extends AbstractProcessor {

    protected Elements elements;

    private List<? extends Element> objectMembers;

    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.objectMembers = elements.getAllMembers(elements.getTypeElement(Object.class.getName()));
    }

    protected List<? extends Element> getActualMembers(TypeElement type) {
        List<? extends Element> members = new LinkedList<>(elements.getAllMembers(type));
        members.removeAll(objectMembers);
        return members;
    }

    protected List<? extends ExecutableElement> getActualMethods(TypeElement type) {
        return methodsIn(getActualMembers(type));
    }

    protected Map<String, ExecutableElement> getActualMethodsMap(TypeElement type) {
        Map<String, ExecutableElement> methodsMap = new HashMap<>();
        getActualMethods(type).forEach(method -> {
            methodsMap.put(method.toString(), method);
        });
        return methodsMap;
    }

    public static String getMethodSignature(ExecutableElement method) {
        if (!ElementKind.METHOD.equals(method.getKind())) {
            throw new IllegalArgumentException("The argument must be Method Kind");
        }

        StringBuilder methodSignatureBuilder = new StringBuilder();

        method.getModifiers().forEach(member -> {
            methodSignatureBuilder.append(member).append(' ');
        });

        methodSignatureBuilder.append(method.getReturnType())
                .append(' ')
                .append(method.toString());

        return methodSignatureBuilder.toString();
    }

    protected TypeElement getTypeElement(CharSequence className) {
        return elements.getTypeElement(className);
    }

    protected PackageElement getPackageElement(Element type) {
        return this.elements.getPackageOf(type);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public final Set<String> getSupportedAnnotationTypes() {
        return SUPPORTED_ANNOTATION_TYPES;
    }
}
