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
package org.apache.dubbo.config.spring.context.annotation;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.springframework.context.annotation.AnnotationConfigUtils.registerAnnotationConfigProcessors;

/**
 * Dubbo {@link ClassPathBeanDefinitionScanner} that exposes some methods to be public.
 *
 * @see #doScan(String...)
 * @see #registerDefaultFilters()
 * @since 2.5.7
 */
public class DubboClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    private final ConcurrentMap<String, Set<BeanDefinition>> beanDefinitionMap = new ConcurrentHashMap<>();

    public DubboClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters, Environment environment,
                                               ResourceLoader resourceLoader) {

        super(registry, useDefaultFilters);

        setEnvironment(environment);

        setResourceLoader(resourceLoader);

        registerAnnotationConfigProcessors(registry);

    }

    public DubboClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, Environment environment,
                                               ResourceLoader resourceLoader) {

        this(registry, false, environment, resourceLoader);

    }

    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        return super.doScan(basePackages);
    }

    @Override
    public boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
        return super.checkCandidate(beanName, beanDefinition);
    }

    @Override
    public Set<BeanDefinition> findCandidateComponents(String basePackage) {
        Set<BeanDefinition> beanDefinitions = beanDefinitionMap.get(basePackage);
        if (CollectionUtils.isEmpty(beanDefinitions)) {
            beanDefinitions = super.findCandidateComponents(basePackage);
            beanDefinitionMap.put(basePackage, beanDefinitions);
        }
        return beanDefinitions;
    }

    /**
     * Registers @Service Bean and Finds all BeanDefinitionHolders of @Service
     *
     * @param registry              BeanDefinitionRegistry
     * @param beanNameGenerator     BeanNameGenerator
     * @param basePackage           basePackage
     * @return                      all BeanDefinitionHolders of @Service
     */
    public Set<BeanDefinitionHolder> doScan(BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator, String basePackage) {
        // Registers @Service Bean first
        super.doScan(basePackage);
        Set<BeanDefinition> beanDefinitions = findCandidateComponents(basePackage);
        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<BeanDefinitionHolder>(beanDefinitions.size());
        for (BeanDefinition beanDefinition : beanDefinitions) {
            String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
            BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
            beanDefinitionHolders.add(beanDefinitionHolder);
        }
        return beanDefinitionHolders;
    }

}
