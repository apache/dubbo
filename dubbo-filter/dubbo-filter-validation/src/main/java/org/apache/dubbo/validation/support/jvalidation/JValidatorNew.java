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
package org.apache.dubbo.validation.support.jvalidation;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.bytecode.ClassGenerator;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.validation.MethodValidated;
import org.apache.dubbo.validation.Validator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FILTER_VALIDATION_EXCEPTION;

/**
 * Implementation of JValidationNew. JValidationNew is invoked if configuration validation attribute value is 'jvalidationNew'.
 * <pre>
 *     e.g. &lt;dubbo:method name="save" validation="jvalidationNew" /&gt;
 * </pre>
 */
public class JValidatorNew implements Validator {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(JValidatorNew.class);

    private final Class<?> clazz;

    private final Map<String, Class<?>> methodClassMap;

    private final jakarta.validation.Validator validator;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public JValidatorNew(URL url) {
        this.clazz = ReflectUtils.forName(url.getServiceInterface());
        String jvalidation = url.getParameter("jvalidationNew");
        ValidatorFactory factory;
        if (StringUtils.isNotEmpty(jvalidation)) {
            factory = Validation.byProvider((Class) ReflectUtils.forName(jvalidation))
                    .configure()
                    .buildValidatorFactory();
        } else {
            factory = Validation.buildDefaultValidatorFactory();
        }
        this.validator = factory.getValidator();
        this.methodClassMap = new ConcurrentHashMap<>();
    }

    private static Object getMethodParameterBean(Class<?> clazz, Method method, Object[] args) {
        if (!hasConstraintParameter(method)) {
            return null;
        }
        try {
            String parameterClassName = generateMethodParameterClassName(clazz, method);
            Class<?> parameterClass;
            try {
                parameterClass = Class.forName(parameterClassName, true, clazz.getClassLoader());
            } catch (ClassNotFoundException e) {
                parameterClass = generateMethodParameterClass(clazz, method, parameterClassName);
            }
            Object parameterBean = parameterClass.getDeclaredConstructor().newInstance();
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Field field = parameterClass.getField(parameters[i].getName());
                field.set(parameterBean, args[i]);
            }
            return parameterBean;
        } catch (Throwable e) {
            logger.warn(CONFIG_FILTER_VALIDATION_EXCEPTION, "", "", e.getMessage(), e);
            return null;
        }
    }

    /**
     * try to generate methodParameterClass.
     *
     * @param clazz              interface class
     * @param method             invoke method
     * @param parameterClassName generated parameterClassName
     * @return Class<?> generated methodParameterClass
     */
    private static Class<?> generateMethodParameterClass(Class<?> clazz, Method method, String parameterClassName)
            throws Exception {
        ClassPool pool = ClassGenerator.getClassPool(clazz.getClassLoader());
        synchronized (parameterClassName.intern()) {
            CtClass ctClass = null;
            try {
                ctClass = pool.getCtClass(parameterClassName);
            } catch (NotFoundException ignore) {
            }

            if (null == ctClass) {
                ctClass = pool.makeClass(parameterClassName);
                ClassFile classFile = ctClass.getClassFile();
                ctClass.addConstructor(CtNewConstructor.defaultConstructor(pool.getCtClass(parameterClassName)));
                // parameter fields
                Parameter[] parameters = method.getParameters();
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                for (int i = 0; i < parameters.length; i++) {
                    Annotation[] annotations = parameterAnnotations[i];
                    AnnotationsAttribute attribute =
                            new AnnotationsAttribute(classFile.getConstPool(), AnnotationsAttribute.visibleTag);
                    for (Annotation annotation : annotations) {
                        if (annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                            javassist.bytecode.annotation.Annotation ja = new javassist.bytecode.annotation.Annotation(
                                    classFile.getConstPool(),
                                    pool.getCtClass(annotation.annotationType().getName()));
                            Method[] members = annotation.annotationType().getMethods();
                            for (Method member : members) {
                                if (Modifier.isPublic(member.getModifiers())
                                        && member.getParameterTypes().length == 0
                                        && member.getDeclaringClass() == annotation.annotationType()) {
                                    Object value = member.invoke(annotation);
                                    if (null != value) {
                                        MemberValue memberValue = createMemberValue(
                                                classFile.getConstPool(),
                                                pool.get(member.getReturnType().getName()),
                                                value);
                                        ja.addMemberValue(member.getName(), memberValue);
                                    }
                                }
                            }
                            attribute.addAnnotation(ja);
                        }
                    }
                    Parameter parameter = parameters[i];
                    Class<?> type = parameter.getType();
                    String fieldName = parameter.getName();
                    CtField ctField = CtField.make(
                            "public " + type.getCanonicalName() + " " + fieldName + ";",
                            pool.getCtClass(parameterClassName));
                    ctField.getFieldInfo().addAttribute(attribute);
                    ctClass.addField(ctField);
                }
                return pool.toClass(ctClass, clazz, clazz.getClassLoader(), clazz.getProtectionDomain());
            } else {
                return Class.forName(parameterClassName, true, clazz.getClassLoader());
            }
        }
    }

    private static String generateMethodParameterClassName(Class<?> clazz, Method method) {
        StringBuilder builder = new StringBuilder()
                .append(clazz.getName())
                .append('_')
                .append(toUpperMethodName(method.getName()))
                .append("Parameter");

        Class<?>[] parameterTypes = method.getParameterTypes();
        for (Class<?> parameterType : parameterTypes) {
            // In order to ensure that the parameter class can be generated correctly,
            // replace "." with "_" to make the package name of the generated parameter class
            // consistent with the package name of the actual parameter class.
            builder.append('_').append(parameterType.getName().replace(".", "_"));
        }

        return builder.toString();
    }

    private static boolean hasConstraintParameter(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (Annotation[] annotations : parameterAnnotations) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String toUpperMethodName(String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    // Copy from javassist.bytecode.annotation.Annotation.createMemberValue(ConstPool, CtClass);
    private static MemberValue createMemberValue(ConstPool cp, CtClass type, Object value) throws NotFoundException {
        MemberValue memberValue = javassist.bytecode.annotation.Annotation.createMemberValue(cp, type);
        if (memberValue instanceof BooleanMemberValue) {
            ((BooleanMemberValue) memberValue).setValue((Boolean) value);
        } else if (memberValue instanceof ByteMemberValue) {
            ((ByteMemberValue) memberValue).setValue((Byte) value);
        } else if (memberValue instanceof CharMemberValue) {
            ((CharMemberValue) memberValue).setValue((Character) value);
        } else if (memberValue instanceof ShortMemberValue) {
            ((ShortMemberValue) memberValue).setValue((Short) value);
        } else if (memberValue instanceof IntegerMemberValue) {
            ((IntegerMemberValue) memberValue).setValue((Integer) value);
        } else if (memberValue instanceof LongMemberValue) {
            ((LongMemberValue) memberValue).setValue((Long) value);
        } else if (memberValue instanceof FloatMemberValue) {
            ((FloatMemberValue) memberValue).setValue((Float) value);
        } else if (memberValue instanceof DoubleMemberValue) {
            ((DoubleMemberValue) memberValue).setValue((Double) value);
        } else if (memberValue instanceof ClassMemberValue) {
            ((ClassMemberValue) memberValue).setValue(((Class<?>) value).getName());
        } else if (memberValue instanceof StringMemberValue) {
            ((StringMemberValue) memberValue).setValue((String) value);
        } else if (memberValue instanceof EnumMemberValue) {
            ((EnumMemberValue) memberValue).setValue(((Enum<?>) value).name());
        }
        /* else if (memberValue instanceof AnnotationMemberValue) */
        else if (memberValue instanceof ArrayMemberValue) {
            CtClass arrayType = type.getComponentType();
            int len = Array.getLength(value);
            MemberValue[] members = new MemberValue[len];
            for (int i = 0; i < len; i++) {
                members[i] = createMemberValue(cp, arrayType, Array.get(value, i));
            }
            ((ArrayMemberValue) memberValue).setValue(members);
        }
        return memberValue;
    }

    @Override
    public void validate(String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Exception {
        List<Class<?>> groups = new ArrayList<>();
        Class<?> methodClass = methodClass(methodName);
        if (methodClass != null) {
            groups.add(methodClass);
        }

        Method method = clazz.getMethod(methodName, parameterTypes);
        Class<?>[] methodClasses;
        if (method.isAnnotationPresent(MethodValidated.class)) {
            methodClasses = method.getAnnotation(MethodValidated.class).value();
            groups.addAll(Arrays.asList(methodClasses));
        }
        // add into default group
        groups.add(0, Default.class);
        groups.add(1, clazz);

        // convert list to array
        Class<?>[] classGroups = groups.toArray(new Class[0]);

        Set<ConstraintViolation<?>> violations = new HashSet<>();
        Object parameterBean = getMethodParameterBean(clazz, method, arguments);
        if (parameterBean != null) {
            violations.addAll(validator.validate(parameterBean, classGroups));
        }

        for (Object arg : arguments) {
            validate(violations, arg, classGroups);
        }

        if (!violations.isEmpty()) {
            logger.info("Failed to validate service: " + clazz.getName() + ", method: " + methodName + ", cause: "
                    + violations);
            throw new ConstraintViolationException(
                    "Failed to validate service: " + clazz.getName() + ", method: " + methodName + ", cause: "
                            + violations,
                    violations);
        }
    }

    private Class<?> methodClass(String methodName) {
        Class<?> methodClass = null;
        String methodClassName = clazz.getName() + "$" + toUpperMethodName(methodName);
        Class<?> cached = methodClassMap.get(methodClassName);
        if (cached != null) {
            return cached == clazz ? null : cached;
        }
        try {
            methodClass =
                    Class.forName(methodClassName, false, Thread.currentThread().getContextClassLoader());
            methodClassMap.put(methodClassName, methodClass);
        } catch (ClassNotFoundException e) {
            methodClassMap.put(methodClassName, clazz);
        }
        return methodClass;
    }

    private void validate(Set<ConstraintViolation<?>> violations, Object arg, Class<?>... groups) {
        if (arg != null && !ReflectUtils.isPrimitives(arg.getClass())) {
            if (arg instanceof Object[]) {
                for (Object item : (Object[]) arg) {
                    validate(violations, item, groups);
                }
            } else if (arg instanceof Collection) {
                for (Object item : (Collection<?>) arg) {
                    validate(violations, item, groups);
                }
            } else if (arg instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) arg).entrySet()) {
                    validate(violations, entry.getKey(), groups);
                    validate(violations, entry.getValue(), groups);
                }
            } else {
                violations.addAll(validator.validate(arg, groups));
            }
        }
    }
}
