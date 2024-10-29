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
package org.apache.dubbo.config.spring6.beans.factory.annotation;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import org.apache.dubbo.config.spring.context.event.DubboConfigInitEvent;
import org.apache.dubbo.config.spring.util.SpringCompatUtils;
import org.apache.dubbo.config.spring6.beans.factory.aot.ReferencedFieldValueResolver;
import org.apache.dubbo.config.spring6.beans.factory.aot.ReferencedMethodArgumentsResolver;
import org.apache.dubbo.config.spring6.utils.AotUtils;
import org.apache.dubbo.rpc.service.Destroyable;
import org.apache.dubbo.rpc.service.EchoService;
import org.apache.dubbo.rpc.service.GenericService;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.support.ClassHintUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.aot.AutowiredArgumentsCodeGenerator;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_DUBBO_BEAN_INITIALIZER;

/**
 * The purpose of implementing {@link BeanRegistrationAotProcessor} is to
 * supplement for {@link ReferenceAnnotationBeanPostProcessor} ability of AOT.
 *
 * @since 3.3
 */
public class ReferenceAnnotationWithAotBeanPostProcessor extends ReferenceAnnotationBeanPostProcessor
        implements BeanRegistrationAotProcessor {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    @Nullable
    private ConfigurableListableBeanFactory beanFactory;

    /**
     * {@link com.alibaba.dubbo.config.annotation.Reference @com.alibaba.dubbo.config.annotation.Reference} has been supported since 2.7.3
     * <p>
     * {@link DubboReference @DubboReference} has been supported since 2.7.7
     */
    public ReferenceAnnotationWithAotBeanPostProcessor() {
        super();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Class<?> beanType;
            if (beanFactory.isFactoryBean(beanName)) {
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                if (isReferenceBean(beanDefinition)) {
                    continue;
                }
                if (isAnnotatedReferenceBean(beanDefinition)) {
                    // process @DubboReference at java-config @bean method
                    processReferenceAnnotatedBeanDefinition(beanName, (AnnotatedBeanDefinition) beanDefinition);
                    continue;
                }

                String beanClassName = beanDefinition.getBeanClassName();
                beanType = ClassUtils.resolveClass(beanClassName, getClassLoader());
            } else {
                beanType = beanFactory.getType(beanName);
            }
            if (beanType != null) {
                AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanName, beanType, null);
                try {
                    prepareInjection(metadata);
                } catch (BeansException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalStateException("Prepare dubbo reference injection element failed", e);
                }
            }
        }

        try {
            // this is an early event, it will be notified at
            // org.springframework.context.support.AbstractApplicationContext.registerListeners()
            applicationContext.publishEvent(new DubboConfigInitEvent(applicationContext));
        } catch (Exception e) {
            // if spring version is less than 4.2, it does not support early application event
            logger.warn(
                    CONFIG_DUBBO_BEAN_INITIALIZER,
                    "",
                    "",
                    "publish early application event failed, please upgrade spring version to 4.2.x or later: " + e);
        }
    }

    /**
     * check whether is @DubboReference at java-config @bean method
     */
    private boolean isAnnotatedReferenceBean(BeanDefinition beanDefinition) {
        if (beanDefinition instanceof AnnotatedBeanDefinition) {
            AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
            String beanClassName = SpringCompatUtils.getFactoryMethodReturnType(annotatedBeanDefinition);
            if (beanClassName != null && ReferenceBean.class.getName().equals(beanClassName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReferenceBean(BeanDefinition beanDefinition) {
        return ReferenceBean.class.getName().equals(beanDefinition.getBeanClassName());
    }

    @Override
    @Nullable
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
        Class<?> beanClass = registeredBean.getBeanClass();
        String beanName = registeredBean.getBeanName();
        RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
        AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanDefinition, beanClass, beanName);
        if (!CollectionUtils.isEmpty(metadata.getFieldElements())
                || !CollectionUtils.isEmpty(metadata.getMethodElements())) {
            return new AotContribution(beanClass, metadata, getAutowireCandidateResolver());
        }
        return null;
    }

    private AnnotatedInjectionMetadata findInjectionMetadata(
            RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanName, beanType, null);
        metadata.checkConfigMembers(beanDefinition);
        return metadata;
    }

    @Nullable
    private AutowireCandidateResolver getAutowireCandidateResolver() {
        if (this.beanFactory instanceof DefaultListableBeanFactory) {
            return ((DefaultListableBeanFactory) this.beanFactory).getAutowireCandidateResolver();
        }
        return null;
    }

    private static class AotContribution implements BeanRegistrationAotContribution {

        private static final String REGISTERED_BEAN_PARAMETER = "registeredBean";

        private static final String INSTANCE_PARAMETER = "instance";

        private final Class<?> target;

        private final AnnotatedInjectionMetadata annotatedInjectionMetadata;

        @Nullable
        private final AutowireCandidateResolver candidateResolver;

        AotContribution(
                Class<?> target,
                AnnotatedInjectionMetadata annotatedInjectionMetadata,
                AutowireCandidateResolver candidateResolver) {

            this.target = target;
            this.annotatedInjectionMetadata = annotatedInjectionMetadata;
            this.candidateResolver = candidateResolver;
        }

        @Override
        public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
            GeneratedClass generatedClass = generationContext
                    .getGeneratedClasses()
                    .addForFeatureComponent("DubboReference", this.target, type -> {
                        type.addJavadoc("DubboReference for {@link $T}.", this.target);
                        type.addModifiers(javax.lang.model.element.Modifier.PUBLIC);
                    });
            GeneratedMethod generateMethod = generatedClass.getMethods().add("apply", method -> {
                method.addJavadoc("Apply the dubbo reference.");
                method.addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.STATIC);
                method.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER);
                method.addParameter(this.target, INSTANCE_PARAMETER);
                method.returns(this.target);
                method.addCode(generateMethodCode(generatedClass.getName(), generationContext.getRuntimeHints()));
            });
            beanRegistrationCode.addInstancePostProcessor(generateMethod.toMethodReference());

            if (this.candidateResolver != null) {
                registerHints(generationContext.getRuntimeHints());
            }
        }

        private CodeBlock generateMethodCode(ClassName targetClassName, RuntimeHints hints) {
            CodeBlock.Builder code = CodeBlock.builder();
            if (!CollectionUtils.isEmpty(this.annotatedInjectionMetadata.getFieldElements())) {
                for (AnnotatedInjectElement referenceElement : this.annotatedInjectionMetadata.getFieldElements()) {
                    code.addStatement(generateStatementForElement(targetClassName, referenceElement, hints));
                }
            }
            if (!CollectionUtils.isEmpty(this.annotatedInjectionMetadata.getMethodElements())) {
                for (AnnotatedInjectElement referenceElement : this.annotatedInjectionMetadata.getMethodElements()) {
                    code.addStatement(generateStatementForElement(targetClassName, referenceElement, hints));
                }
            }
            code.addStatement("return $L", INSTANCE_PARAMETER);
            return code.build();
        }

        private CodeBlock generateStatementForElement(
                ClassName targetClassName, AnnotatedInjectElement referenceElement, RuntimeHints hints) {

            Member member = referenceElement.getMember();
            AnnotationAttributes attributes = referenceElement.attributes;
            Object injectedObject = referenceElement.injectedObject;

            try {
                Class<?> c = referenceElement.getInjectedType();
                AotUtils.registerSerializationForService(c, hints);
                hints.reflection().registerType(TypeReference.of(c), MemberCategory.INVOKE_PUBLIC_METHODS);
                // need to enumerate all interfaces by the proxy
                hints.proxies().registerJdkProxy(c, EchoService.class, Destroyable.class);
                hints.proxies().registerJdkProxy(c, EchoService.class, Destroyable.class, GenericService.class);
                hints.proxies()
                        .registerJdkProxy(
                                c,
                                EchoService.class,
                                Destroyable.class,
                                SpringProxy.class,
                                Advised.class,
                                DecoratingProxy.class);
                hints.proxies()
                        .registerJdkProxy(
                                c,
                                EchoService.class,
                                GenericService.class,
                                Destroyable.class,
                                SpringProxy.class,
                                Advised.class,
                                DecoratingProxy.class);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            if (member instanceof Field) {
                return generateMethodStatementForField(
                        targetClassName, (Field) member, attributes, injectedObject, hints);
            }
            if (member instanceof Method) {
                return generateMethodStatementForMethod(
                        targetClassName, (Method) member, attributes, injectedObject, hints);
            }
            throw new IllegalStateException(
                    "Unsupported member type " + member.getClass().getName());
        }

        private CodeBlock generateMethodStatementForField(
                ClassName targetClassName,
                Field field,
                AnnotationAttributes attributes,
                Object injectedObject,
                RuntimeHints hints) {

            hints.reflection().registerField(field);
            CodeBlock resolver =
                    CodeBlock.of("$T.$L($S)", ReferencedFieldValueResolver.class, "forRequiredField", field.getName());
            CodeBlock shortcutResolver = CodeBlock.of("$L.withShortcut($S)", resolver, injectedObject);
            AccessControl accessControl = AccessControl.forMember(field);

            if (!accessControl.isAccessibleFrom(targetClassName)) {
                return CodeBlock.of(
                        "$L.resolveAndSet($L, $L)", shortcutResolver, REGISTERED_BEAN_PARAMETER, INSTANCE_PARAMETER);
            }
            return CodeBlock.of(
                    "$L.$L = $L.resolve($L)",
                    INSTANCE_PARAMETER,
                    field.getName(),
                    shortcutResolver,
                    REGISTERED_BEAN_PARAMETER);
        }

        private CodeBlock generateMethodStatementForMethod(
                ClassName targetClassName,
                Method method,
                AnnotationAttributes attributes,
                Object injectedObject,
                RuntimeHints hints) {

            CodeBlock.Builder code = CodeBlock.builder();
            code.add("$T.$L", ReferencedMethodArgumentsResolver.class, "forRequiredMethod");
            code.add("($S", method.getName());
            if (method.getParameterCount() > 0) {
                code.add(", $L", generateParameterTypesCode(method.getParameterTypes()));
            }
            code.add(")");
            if (method.getParameterCount() > 0) {
                Parameter[] parameters = method.getParameters();
                String[] parameterNames = new String[parameters.length];
                for (int i = 0; i < parameterNames.length; i++) {
                    parameterNames[i] = parameters[i].getName();
                }
                code.add(".withShortcut($L)", generateParameterNamesCode(parameterNames));
            }
            AccessControl accessControl = AccessControl.forMember(method);
            if (!accessControl.isAccessibleFrom(targetClassName)) {
                hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
                code.add(".resolveAndInvoke($L, $L)", REGISTERED_BEAN_PARAMETER, INSTANCE_PARAMETER);
            } else {
                hints.reflection().registerMethod(method, ExecutableMode.INTROSPECT);
                CodeBlock arguments = new AutowiredArgumentsCodeGenerator(this.target, method)
                        .generateCode(method.getParameterTypes());
                CodeBlock injectionCode =
                        CodeBlock.of("args -> $L.$L($L)", INSTANCE_PARAMETER, method.getName(), arguments);
                code.add(".resolve($L, $L)", REGISTERED_BEAN_PARAMETER, injectionCode);
            }
            return code.build();
        }

        private CodeBlock generateParameterNamesCode(String[] parameterNames) {
            CodeBlock.Builder code = CodeBlock.builder();
            for (int i = 0; i < parameterNames.length; i++) {
                code.add(i != 0 ? ", " : "");
                code.add("$S", parameterNames[i]);
            }
            return code.build();
        }

        private CodeBlock generateParameterTypesCode(Class<?>[] parameterTypes) {
            CodeBlock.Builder code = CodeBlock.builder();
            for (int i = 0; i < parameterTypes.length; i++) {
                code.add(i != 0 ? ", " : "");
                code.add("$T.class", parameterTypes[i]);
            }
            return code.build();
        }

        private void registerHints(RuntimeHints runtimeHints) {
            if (!CollectionUtils.isEmpty(this.annotatedInjectionMetadata.getFieldElements())) {
                for (AnnotatedInjectElement referenceElement : this.annotatedInjectionMetadata.getFieldElements()) {
                    Member member = referenceElement.getMember();
                    if (member instanceof Field) {
                        Field field = (Field) member;
                        DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(field, true);
                        registerProxyIfNecessary(runtimeHints, dependencyDescriptor);
                    }
                }
            }
            if (!CollectionUtils.isEmpty(this.annotatedInjectionMetadata.getMethodElements())) {
                for (AnnotatedInjectElement referenceElement : this.annotatedInjectionMetadata.getMethodElements()) {
                    Member member = referenceElement.getMember();
                    if (member instanceof Method) {
                        Method method = (Method) member;
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        for (int i = 0; i < parameterTypes.length; i++) {
                            MethodParameter methodParam = new MethodParameter(method, i);
                            DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(methodParam, true);
                            registerProxyIfNecessary(runtimeHints, dependencyDescriptor);
                        }
                    }
                }
            }
        }

        private void registerProxyIfNecessary(RuntimeHints runtimeHints, DependencyDescriptor dependencyDescriptor) {
            if (this.candidateResolver != null) {
                Class<?> proxyClass = this.candidateResolver.getLazyResolutionProxyClass(dependencyDescriptor, null);
                if (proxyClass != null) {
                    ClassHintUtils.registerProxyIfNecessary(proxyClass, runtimeHints);
                }
            }
        }
    }
}
