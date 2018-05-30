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
package com.alibaba.dubbo.config.async;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("com.alibaba.dubbo.async.DubboAsync")
public class AsyncAnnotationProcessor extends AbstractProcessor {

    private static final String OBJECT_NAME = "java.lang.Object";

    private static final String FUTURE_NAME = "ListenableFuture";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(DubboAsync.class);
        if (elements == null || elements.isEmpty()) return false;

        for (Element element : elements) {
            if (element.getKind() == ElementKind.INTERFACE) {
                generateAsyncInterface((TypeElement) element);
            }
        }

        return false;
    }

    private void generateAsyncInterface(TypeElement element) {
        Name qualified = element.getQualifiedName();
        if (qualified == null) return;
        String qualifiedName = qualified.toString();
        if (qualifiedName.length() == 0) return;


        String className = element.getSimpleName().toString();
        PackageElement packageElement = (PackageElement) element.getEnclosingElement();
        String packageName = packageElement.getQualifiedName().toString();


        StringBuilder result = new StringBuilder();
        startAsyncInterface(result, qualifiedName, className, packageName);
        generateAsyncMethods(result, element);
        endAsyncInterface(result);

        Writer writer = null;
        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(qualifiedName + "Async");
            if (sourceFile.getLastModified() > 0) return;

            writer = sourceFile.openWriter();
            writer.write(result.toString());
            writer.close();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "生成async接口失败");
        } finally {
            close(writer);
        }
    }

    private void generateAsyncMethods(StringBuilder result, TypeElement typeElement) {
        List<? extends Element> elements = typeElement.getEnclosedElements();
        Set<String> existMethods = extractExistMethods(elements);
        for (Element element : elements) {
            if (element.getKind() != ElementKind.METHOD) continue;
            generateAsyncMethod(result, (ExecutableElement) element, existMethods);
        }
    }

    private Set<String> extractExistMethods(List<? extends Element> elements) {
        Set<String> result = new HashSet<String>();
        for (Element element : elements) {
            result.add(element.getSimpleName().toString());
        }
        return result;
    }

    private void generateAsyncMethod(StringBuilder result, ExecutableElement element, Set<String> existMethods) {
        String asyncMethodName = element.getSimpleName().toString() + "Async";
        if (existMethods.contains(asyncMethodName)) return;

        appendTypeParameters(result, element);
        appendReturnType(result, element);
        result.append(asyncMethodName).append('(');
        appendParameters(result, element);
        result.append(");");
    }

    //泛型参数
    private void appendTypeParameters(StringBuilder result, ExecutableElement element) {
        List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
        if (typeParameters == null || typeParameters.size() == 0) return;

        result.append("<");
        int i = 1;
        for (TypeParameterElement typeParameter : typeParameters) {
            result.append(typeParameter.getSimpleName().toString());
            appendBounds(result, typeParameter);
            if (i++ < typeParameters.size()) result.append(",");
        }
        result.append("> ");
    }

    //返回值
    private void appendReturnType(StringBuilder result, ExecutableElement element) {
        TypeMirror returnType = element.getReturnType();
        if (returnType.getKind() == TypeKind.VOID) {
            result.append(FUTURE_NAME).append(" ");
        } else {
            result.append(FUTURE_NAME).append('<').append(type2ReturnName(returnType)).append("> ");
        }
    }

    //参数
    private void appendParameters(StringBuilder result, ExecutableElement method) {
        List<? extends VariableElement> parameters = method.getParameters();
        int i = 1;
        for (VariableElement element : parameters) {
            result.append(type2ParameterName(element.asType())).append(" ").append(element.getSimpleName().toString());
            if (i++ < parameters.size()) {
                result.append(",");
            }
        }
    }

    private void appendBounds(StringBuilder result, TypeParameterElement typeParameter) {
        List<? extends TypeMirror> bounds = typeParameter.getBounds();
        if (bounds == null || bounds.size() == 0) return;
        if (bounds.size() == 1) {
            String boundName = type2ReturnName(bounds.get(0));
            if (OBJECT_NAME.equals(boundName)) return;
        }

        result.append(" extends ");
        int i = 1;
        for (TypeMirror bound : bounds) {
            String boundName = type2ReturnName(bound);
            result.append(boundName);
            if (i < bounds.size()) result.append(",");
        }
    }

    private String type2ReturnName(TypeMirror typeMirror) {
        TypeKind kind = typeMirror.getKind();
        if (kind == TypeKind.VOID) return "";
        if (kind == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) typeMirror;
            TypeMirror componentType = arrayType.getComponentType();
            return type2ReturnName(componentType) + "[]";
        }
        if (kind == TypeKind.BOOLEAN) {
            return "Boolean";
        }
        if (kind == TypeKind.BYTE) {
            return "Byte";
        }
        if (kind == TypeKind.CHAR) {
            return "Char";
        }
        if (kind == TypeKind.DOUBLE) {
            return "Double";
        }
        if (kind == TypeKind.FLOAT) {
            return "Float";
        }
        if (kind == TypeKind.INT) {
            return "Integer";
        }
        if (kind == TypeKind.LONG) {
            return "Long";
        }
        if (kind == TypeKind.SHORT) {
            return "Short";
        }
        if (kind == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            Element element = declaredType.asElement();
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (typeArguments == null || typeArguments.size() == 0) {
                return typeName(element);
            }

            StringBuilder result = new StringBuilder();
            result.append(typeName(element)).append("<");
            int i = 1;
            for (TypeMirror typeArgument : typeArguments) {
                result.append(type2ReturnName(typeArgument));
                if (i++ < typeArguments.size()) result.append(",");
            }
            result.append(">");
            return result.toString();
        }
        if (kind == TypeKind.TYPEVAR) {
            TypeVariable typeVariable = (TypeVariable) typeMirror;
            return typeVariable.asElement().getSimpleName().toString();
        }
        if (kind == TypeKind.WILDCARD) {
            WildcardType wildcardType = (WildcardType) typeMirror;
            StringBuilder result = new StringBuilder();
            result.append("?");
            appendSuperBound(result, wildcardType.getSuperBound());
            appendExtendsBound(result, wildcardType.getExtendsBound());
            return result.toString();
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, kind.toString());
        return "";
    }

    private void appendExtendsBound(StringBuilder resut, TypeMirror extendsBound) {
        if (extendsBound == null) return;
        String name = type2ReturnName(extendsBound);
        if (name.equals(OBJECT_NAME)) return;
        resut.append(" extends ").append(name);
    }

    private void appendSuperBound(StringBuilder result, TypeMirror superBound) {
        if (superBound == null) return;
        String name = type2ReturnName(superBound);
        if (name.equals(OBJECT_NAME)) return;
        result.append(" super ").append(name);
    }

    private String type2ParameterName(TypeMirror typeMirror) {
        TypeKind kind = typeMirror.getKind();
        if (kind == TypeKind.BOOLEAN) {
            return "boolean";
        }
        if (kind == TypeKind.BYTE) {
            return "byte";
        }
        if (kind == TypeKind.CHAR) {
            return "char";
        }
        if (kind == TypeKind.DOUBLE) {
            return "double";
        }
        if (kind == TypeKind.FLOAT) {
            return "float";
        }
        if (kind == TypeKind.INT) {
            return "int";
        }
        if (kind == TypeKind.LONG) {
            return "long";
        }
        if (kind == TypeKind.SHORT) {
            return "short";
        }
        return type2ReturnName(typeMirror);
    }

    private String typeName(Element element) {
        if (element.getKind() == ElementKind.CLASS
                || element.getKind() == ElementKind.INTERFACE
                || element.getKind() == ElementKind.ENUM) {
            return ((TypeElement) element).getQualifiedName().toString();
        }
        return element.getSimpleName().toString();
    }

    private void endAsyncInterface(StringBuilder result) {
        result.append("\n}");
    }

    private void startAsyncInterface(StringBuilder result, String qualifiedName, String className, String packageName) {
        result.append("package ").append(packageName).append(";\n");
        result.append("import com.google.common.util.concurrent.ListenableFuture;\n");
        result.append("@javax.annotation.Generated(\"com.alibaba.dubbo.async.processor.AsyncAnnotationProcessor\")\n");
        result.append("@com.alibaba.dubbo.config.annotation.AsyncFor(").append(qualifiedName).append(".class)\n");
        result.append("public interface ").append(className).append("Async extends ").append(className).append(" {\n");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    private void close(Writer writer) {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException e) {

        }
    }
}

