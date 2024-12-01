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
package org.apache.dubbo.rpc.protocol.tri.rest.support.swagger;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.LRUCache;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta.ReturnParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPIDefinitionResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPISchemaResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Info;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;

import java.lang.ref.WeakReference;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.github.therapi.runtimejavadoc.ClassJavadoc;
import com.github.therapi.runtimejavadoc.Comment;
import com.github.therapi.runtimejavadoc.CommentFormatter;
import com.github.therapi.runtimejavadoc.FieldJavadoc;
import com.github.therapi.runtimejavadoc.MethodJavadoc;
import com.github.therapi.runtimejavadoc.ParamJavadoc;
import com.github.therapi.runtimejavadoc.RuntimeJavadoc;
import com.github.therapi.runtimejavadoc.internal.MethodSignature;
import com.github.therapi.runtimejavadoc.internal.RuntimeJavadocHelper;

@Activate(order = -10000, onClass = "com.github.therapi.runtimejavadoc.RuntimeJavadoc")
public class JavadocOpenAPIDefinitionResolver implements OpenAPIDefinitionResolver, OpenAPISchemaResolver {

    private final LRUCache<Class<?>, WeakReference<ClassJavadocWrapper>> cache = new LRUCache<>(128);
    private final CommentFormatter formatter = new CommentFormatter();

    @Override
    public OpenAPI resolve(OpenAPI openAPI, ServiceMeta serviceMeta, OpenAPIChain chain) {
        openAPI = chain.resolve(openAPI, serviceMeta);
        if (openAPI == null) {
            return null;
        }

        Info info = openAPI.getInfo();
        if (info == null) {
            openAPI.setInfo(info = new Info());
        }

        if (info.getSummary() != null || info.getDescription() != null) {
            return openAPI;
        }

        ClassJavadoc javadoc = getClassJavadoc(serviceMeta.getType()).javadoc;
        if (javadoc.isEmpty()) {
            return openAPI;
        }

        populateComment(javadoc.getComment(), info::setSummary, info::setDescription);
        return openAPI;
    }

    @Override
    public Collection<HttpMethods> resolve(PathItem pathItem, MethodMeta methodMeta, OperationContext context) {
        return null;
    }

    @Override
    public Operation resolve(Operation operation, MethodMeta methodMeta, OperationContext ctx, OperationChain chain) {
        operation = chain.resolve(operation, methodMeta, ctx);
        if (operation == null) {
            return null;
        }

        Method method = methodMeta.getMethod();
        ClassJavadocWrapper javadoc = getClassJavadoc(method.getDeclaringClass());
        if (javadoc.isEmpty()) {
            return operation;
        }

        if (operation.getSummary() == null && operation.getDescription() == null) {
            MethodJavadoc methodJavadoc = javadoc.getMethod(method);
            if (methodJavadoc != null) {
                populateComment(methodJavadoc.getComment(), operation::setSummary, operation::setDescription);
            }
        }

        List<Parameter> parameters = operation.getParameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                if (parameter.getDescription() != null) {
                    continue;
                }

                ParameterMeta meta = parameter.getMeta();
                if (!(meta instanceof MethodParameterMeta)) {
                    continue;
                }

                populateComment(javadoc.getParameter(method, parameter.getName()), null, parameter::setDescription);
            }
        }

        return operation;
    }

    @Override
    public Schema resolve(ParameterMeta parameter, SchemaContext context, SchemaChain chain) {
        Schema schema = chain.resolve(parameter, context);
        if (schema == null) {
            return null;
        }

        if (schema.getTitle() != null || schema.getDescription() != null) {
            return schema;
        }

        Comment comment = null;
        if (parameter instanceof MethodParameterMeta) {
            MethodParameterMeta meta = (MethodParameterMeta) parameter;
            Method method = meta.getMethod();
            comment = getClassJavadoc(method.getDeclaringClass()).getParameter(method, parameter.getName());
        } else if (parameter instanceof ReturnParameterMeta) {
            ReturnParameterMeta meta = (ReturnParameterMeta) parameter;
            Method method = meta.getMethod();
            MethodJavadoc methodJavadoc =
                    getClassJavadoc(method.getDeclaringClass()).getMethod(method);
            if (methodJavadoc != null) {
                comment = methodJavadoc.getReturns();
            }
        } else {
            for (AnnotatedElement element : parameter.getAnnotatedElements()) {
                if (element instanceof Class) {
                    comment = getClassJavadoc((Class<?>) element).getClassComment();
                } else if (element instanceof Field) {
                    Field field = (Field) element;

                    ClassJavadocWrapper javadoc = getClassJavadoc(field.getDeclaringClass());
                    FieldJavadoc fieldJavadoc = javadoc.getField(field);
                    if (fieldJavadoc != null) {
                        comment = fieldJavadoc.getComment();
                        break;
                    }

                    ParamJavadoc paramJavadoc = javadoc.getRecordComponent(field.getName());
                    if (paramJavadoc != null) {
                        comment = paramJavadoc.getComment();
                        break;
                    }
                } else if (element instanceof Method) {
                    Method method = (Method) element;

                    ClassJavadocWrapper javadoc = getClassJavadoc(method.getDeclaringClass());
                    MethodJavadoc methodJavadoc = javadoc.getMethod(method);

                    if (methodJavadoc != null) {
                        comment = methodJavadoc.getReturns();
                        break;
                    }
                }
            }
        }

        populateComment(comment, schema::setTitle, schema::setDescription);
        return schema;
    }

    private ClassJavadocWrapper getClassJavadoc(Class<?> clazz) {
        WeakReference<ClassJavadocWrapper> ref = cache.get(clazz);
        ClassJavadocWrapper javadoc = ref == null ? null : ref.get();
        if (javadoc == null) {
            javadoc = new ClassJavadocWrapper(RuntimeJavadoc.getJavadoc(clazz));
            cache.put(clazz, new WeakReference<>(javadoc));
        }
        return javadoc;
    }

    private void populateComment(Comment comment, Consumer<String> sConsumer, Consumer<String> dConsumer) {
        if (comment == null) {
            return;
        }

        String description = formatter.format(comment);
        if (sConsumer == null) {
            dConsumer.accept(description);
            return;
        }

        String summary = getFirstSentence(description);
        sConsumer.accept(summary);
        if (description.equals(summary)) {
            return;
        }
        dConsumer.accept(description);
    }

    private static String getFirstSentence(String text) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        int pOpenIndex = text.indexOf("<p>");
        int pCloseIndex = text.indexOf("</p>");
        int dotIndex = text.indexOf(".");
        if (pOpenIndex != -1) {
            if (pOpenIndex == 0 && pCloseIndex != -1) {
                if (dotIndex != -1) {
                    return text.substring(3, Math.min(pCloseIndex, dotIndex));
                }
                return text.substring(3, pCloseIndex);
            }
            if (dotIndex != -1) {
                return text.substring(0, Math.min(pOpenIndex, dotIndex));
            }
            return text.substring(0, pOpenIndex);
        }
        if (dotIndex != -1 && text.length() != dotIndex + 1 && Character.isWhitespace(text.charAt(dotIndex + 1))) {
            return text.substring(0, dotIndex + 1);
        }
        return text;
    }

    private static final class ClassJavadocWrapper {

        private static final Map<Field, Field> MAPPING = new LinkedHashMap<>();
        private static Field PARAMS;

        public final ClassJavadoc javadoc;
        public Map<String, FieldJavadoc> fields;
        public Map<MethodSignature, MethodJavadoc> methods;
        public Map<String, ParamJavadoc> recordComponents;

        static {
            try {
                Field[] fields = ClassJavadoc.class.getDeclaredFields();
                Field[] wFields = ClassJavadocWrapper.class.getFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    for (Field wField : wFields) {
                        if (wField.getName().equals(field.getName())) {
                            MAPPING.put(field, wField);
                            break;
                        }
                    }
                }
                PARAMS = MethodJavadoc.class.getDeclaredField("params");
                PARAMS.setAccessible(true);
            } catch (Throwable ignored) {
            }
        }

        public ClassJavadocWrapper(ClassJavadoc javadoc) {
            this.javadoc = javadoc;
            try {
                for (Map.Entry<Field, Field> entry : MAPPING.entrySet()) {
                    entry.getValue().set(this, entry.getKey().get(javadoc));
                }
            } catch (Throwable ignored) {
            }
        }

        public boolean isEmpty() {
            return javadoc.isEmpty();
        }

        public Comment getClassComment() {
            return javadoc.getComment();
        }

        public FieldJavadoc getField(Field field) {
            if (fields == null) {
                return null;
            }
            FieldJavadoc fieldJavadoc = fields.get(field.getName());
            return fieldJavadoc == null || fieldJavadoc.isEmpty() ? null : fieldJavadoc;
        }

        public MethodJavadoc getMethod(Method method) {
            if (methods == null) {
                return null;
            }
            MethodJavadoc methodJavadoc = methods.get(MethodSignature.from(method));
            if (methodJavadoc != null && !methodJavadoc.isEmpty()) {
                return methodJavadoc;
            }
            Method bridgeMethod = RuntimeJavadocHelper.findBridgeMethod(method);
            if (bridgeMethod != null && bridgeMethod != method) {
                methodJavadoc = methods.get(MethodSignature.from(bridgeMethod));
                if (methodJavadoc != null && !methodJavadoc.isEmpty()) {
                    return methodJavadoc;
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        public Comment getParameter(Method method, String name) {
            if (methods == null) {
                return null;
            }
            MethodJavadoc methodJavadoc = methods.get(MethodSignature.from(method));
            if (methodJavadoc == null || PARAMS == null) {
                return null;
            }
            try {
                Map<String, ParamJavadoc> params = (Map<String, ParamJavadoc>) PARAMS.get(methodJavadoc);
                ParamJavadoc paramJavadoc = params.get(name);
                if (paramJavadoc != null) {
                    return paramJavadoc.getComment();
                }
            } catch (Throwable ignored) {
            }
            return null;
        }

        public ParamJavadoc getRecordComponent(String name) {
            return recordComponents == null ? null : recordComponents.get(name);
        }
    }
}
