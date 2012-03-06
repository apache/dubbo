/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.validation.filter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.AnnotationMemberValue;
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

import javax.validation.Constraint;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.groups.Default;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.bytecode.ClassGenerator;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * ValidationFilter
 * 
 * @author william.liangf
 */
@Activate(group = {Constants.CONSUMER, Constants.PROVIDER}, value = Constants.VALIDATION_KEY)
public class ValidationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ValidationFilter.class);

    private final Validator validator;

    public ValidationFilter() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.VALIDATION_KEY, false)) {
            try {
                Class<?> clazz = invoker.getInterface();
                String methodName = invocation.getMethodName();
                String methodClassName = clazz.getName() + "$" + toUpperMethoName(methodName);
                Class<?> methodClass = null;
                try {
                    methodClass = Class.forName(methodClassName, false, Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException e) {
                }
                Set<ConstraintViolation<?>> violations = new HashSet<ConstraintViolation<?>>();
                Method method = clazz.getMethod(methodName, invocation.getParameterTypes());
                Object parameterBean = getMethodParameterBean(clazz, method, invocation.getArguments());
                if (parameterBean != null) {
                    if (methodClass != null) {
                        violations.addAll(validator.validate(parameterBean, Default.class, clazz, methodClass));
                    } else {
                        violations.addAll(validator.validate(parameterBean, Default.class, clazz));
                    }
                }
                for (Object arg : invocation.getArguments()) {
                    validate(violations, arg, clazz, methodClass);
                }
                if (violations.size() > 0) {
                    ConstraintViolationException e = new ConstraintViolationException("Failed to validate service: " + clazz.getName() + ", method: " + methodName + ", cause: " + violations, violations);
                    throw new RpcException(e.getMessage(), e);
                }
            } catch (RpcException e) {
                throw e;
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
        return invoker.invoke(invocation);
    }
    
    private void validate(Set<ConstraintViolation<?>> violations, Object arg, Class<?> clazz, Class<?> methodClass) {
        if (arg != null && ! isPrimitives(arg.getClass())) {
            if (Object[].class.isInstance(arg)) {
                for (Object item : (Object[]) arg) {
                    validate(violations, item, clazz, methodClass);
                }
            } else if (Collection.class.isInstance(arg)) {
                for (Object item : (Collection<?>) arg) {
                    validate(violations, item, clazz, methodClass);
                }
            } else if (Map.class.isInstance(arg)) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) arg).entrySet()) {
                    validate(violations, entry.getKey(), clazz, methodClass);
                    validate(violations, entry.getValue(), clazz, methodClass);
                }
            } else {
                if (methodClass != null) {
                    violations.addAll(validator.validate(arg, Default.class, clazz, methodClass));
                } else {
                    violations.addAll(validator.validate(arg, Default.class, clazz));
                }
            }
        }
    }
    
    private static boolean isPrimitives(Class<?> cls) {
        if (cls.isArray()) {
            return isPrimitive(cls.getComponentType());
        }
        return isPrimitive(cls);
    }
    
    private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == String.class || cls == Boolean.class || cls == Character.class 
                || Number.class.isAssignableFrom(cls) || Date.class.isAssignableFrom(cls);
    }
    
    private static Object getMethodParameterBean(Class<?> clazz, Method method, Object[] args) {
        if (! hasConstraintParameter(method)) {
            return null;
        }
        try {
            String upperName = toUpperMethoName(method.getName());
            String parameterSimpleName = upperName + "Parameter";
            String parameterClassName = clazz.getName() + "$" + parameterSimpleName;
            Class<?> parameterClass;
            try {
                parameterClass = (Class<?>) Class.forName(parameterClassName, true, clazz.getClassLoader());
            } catch (ClassNotFoundException e) {
                ClassPool pool = ClassGenerator.getClassPool(clazz.getClassLoader());
                CtClass ctClass = pool.makeClass(parameterClassName);
                ClassFile classFile = ctClass.getClassFile();
                classFile.setVersionToJava5();
                ctClass.addConstructor(CtNewConstructor.defaultConstructor(pool.getCtClass(parameterClassName)));
                // parameter fields
                Class<?>[] parameterTypes = method.getParameterTypes();
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                for (int i = 0; i < parameterTypes.length; i ++) {
                    Class<?> type = parameterTypes[i];
                    Annotation[] annotations = parameterAnnotations[i];
                    AnnotationsAttribute attribute = new AnnotationsAttribute(classFile.getConstPool(), AnnotationsAttribute.visibleTag);
                    for (Annotation annotation : annotations) {
                        if (annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                            javassist.bytecode.annotation.Annotation ja = new javassist.bytecode.annotation.Annotation(
                                    classFile.getConstPool(), pool.get(annotation.annotationType().getName()));
                            Method[] members = annotation.annotationType().getMethods();
                            for (Method member : members) {
                                if (Modifier.isPublic(member.getModifiers())
                                        && member.getParameterTypes().length == 0
                                        && member.getDeclaringClass() == annotation.annotationType()) {
                                    Object value = member.invoke(annotation, new Object[0]);
                                    MemberValue memberValue = createMemberValue(
                                            classFile.getConstPool(), pool.getCtClass(member.getReturnType().getCanonicalName()), value);
                                    ja.addMemberValue(member.getName(), memberValue);
                                }
                            }
                            attribute.addAnnotation(ja);
                        }
                    }
                    String fieldName = method.getName() + "Argument" + i;
                    CtField ctField = CtField.make("public " + type.getCanonicalName() + " " + fieldName + ";", pool.getCtClass(parameterClassName));
                    ctField.getFieldInfo().addAttribute(attribute);
                    ctClass.addField(ctField);
                }
                parameterClass = ctClass.toClass();
            }
            Object parameterBean = parameterClass.newInstance();
            for (int i = 0; i < args.length; i ++) {
                Field field = parameterClass.getField(method.getName() + "Argument" + i);
                field.set(parameterBean, args[0]);
            }
            return parameterBean;
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
            return null;
        }
    }

    private static boolean hasConstraintParameter(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations != null && parameterAnnotations.length > 0) {
            for (Annotation[] annotations : parameterAnnotations) {
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String toUpperMethoName(String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }
    
    // Copy from javassist.bytecode.annotation.Annotation.createMemberValue(ConstPool, CtClass);
    private static MemberValue createMemberValue(ConstPool cp, CtClass type, Object value) throws NotFoundException {
        if (type == CtClass.booleanType)
            return new BooleanMemberValue((Boolean) value, cp);
        else if (type == CtClass.byteType)
            return new ByteMemberValue((Byte) value, cp);
        else if (type == CtClass.charType)
            return new CharMemberValue((Character) value, cp);
        else if (type == CtClass.shortType)
            return new ShortMemberValue((Short) value, cp);
        else if (type == CtClass.intType)
            return new IntegerMemberValue((Integer) value, cp);
        else if (type == CtClass.longType)
            return new LongMemberValue((Long) value, cp);
        else if (type == CtClass.floatType)
            return new FloatMemberValue((Float) value, cp);
        else if (type == CtClass.doubleType)
            return new DoubleMemberValue((Double) value, cp);
        else if (type.getName().equals("java.lang.Class"))
            return new ClassMemberValue(((Class<?>) value).getName(), cp);
        else if (type.getName().equals("java.lang.String"))
            return new StringMemberValue((String) value, cp);
        else if (type.isArray()) {
            CtClass arrayType = type.getComponentType();
            int len = Array.getLength(value);
            MemberValue[] members = new MemberValue[len];
            for (int i = 0; i < len; i ++) {
                members[i] = createMemberValue(cp, arrayType, Array.get(value, i));
            }
            ArrayMemberValue arrayMemberValue = new ArrayMemberValue(cp);
            arrayMemberValue.setValue(members);
            return arrayMemberValue;
        } else if (type.isInterface()) {
            javassist.bytecode.annotation.Annotation info = new javassist.bytecode.annotation.Annotation(cp, type);
            return new AnnotationMemberValue(info, cp);
        } else {
            // treat as enum.  I know this is not typed,
            // but JBoss has an Annotation Compiler for JDK 1.4
            // and I want it to work with that. - Bill Burke
            EnumMemberValue emv = new EnumMemberValue(cp);
            emv.setType(type.getName());
            return emv;
        }
    }
    
}
