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
package org.apache.dubbo.config.spring.reference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.PropertyResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ReferenceBeanManager implements ApplicationContextAware {
    public static final String BEAN_NAME = "dubboReferenceBeanManager";
    private final Log logger = LogFactory.getLog(getClass());
    //reference bean id/name -> ReferenceBean
    private Map<String, ReferenceBean> referenceIdMap = new ConcurrentHashMap<>();

    //reference key -> [ reference bean names ]
    private Map<String, List<String>> referenceKeyMap = new ConcurrentHashMap<>();

    //reference key -> ReferenceConfig instance
    private Map<String, ReferenceConfig> referenceConfigMap = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;
    private volatile boolean initialized = false;

    public void addReference(ReferenceBean referenceBean) throws Exception {
        String referenceBeanName = referenceBean.getId();
        Assert.notEmptyString(referenceBeanName, "The id of ReferenceBean cannot be empty");
        PropertyResolver propertyResolver = applicationContext.getEnvironment();

        if (!initialized) {
            //TODO add issue url to describe early initialization
            logger.warn("Early initialize reference bean before DubboConfigInitializationPostProcessor," +
                    " the BeanPostProcessor has not been loaded at this time, which may cause abnormalities in some components (such as seata): " +
                    referenceBeanName + " = " + ReferenceBeanSupport.generateReferenceKey(referenceBean, propertyResolver));
        }

        String referenceKey = ReferenceBeanSupport.generateReferenceKey(referenceBean, propertyResolver);
        ReferenceBean oldReferenceBean = referenceIdMap.get(referenceBeanName);
        if (oldReferenceBean != null) {
            if (referenceBean != oldReferenceBean) {
                String oldReferenceKey = ReferenceBeanSupport.generateReferenceKey(oldReferenceBean, propertyResolver);
                throw new IllegalStateException("Found duplicated ReferenceBean with id: " + referenceBeanName +
                        ", old: " + oldReferenceKey + ", new: " + referenceKey);
            }
            return;
        }
        referenceIdMap.put(referenceBeanName, referenceBean);
        //save cache, map reference key to referenceBeanName
        this.registerReferenceKeyAndBeanName(referenceKey, referenceBeanName);

        // if add reference after prepareReferenceBeans(), should init it immediately.
        if (initialized) {
            initReferenceBean(referenceBean);
        }
    }

    public void registerReferenceKeyAndBeanName(String referenceKey, String referenceBeanName) {
        referenceKeyMap.getOrDefault(referenceKey, new ArrayList<>()).add(referenceBeanName);
    }

    public ReferenceBean getById(String key) {
        return referenceIdMap.get(key);
    }

    public List<String> getByKey(String key) {
        return Collections.unmodifiableList(referenceKeyMap.getOrDefault(key, new ArrayList<>()));
    }

    public Collection<ReferenceBean> getReferences() {
        return referenceIdMap.values();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Initialize all reference beans, call at Dubbo starting
     *
     * @throws Exception
     */
    public void prepareReferenceBeans() throws Exception {
        initialized = true;
        for (ReferenceBean referenceBean : getReferences()) {
            initReferenceBean(referenceBean);
        }

        // prepare all reference beans, including those loaded very early that are dependent on some BeanFactoryPostProcessor
//        Map<String, ReferenceBean> referenceBeanMap = applicationContext.getBeansOfType(ReferenceBean.class, true, false);
//        for (ReferenceBean referenceBean : referenceBeanMap.values()) {
//            addReference(referenceBean);
//        }
    }

    /**
     * NOTE: This method should only call after all dubbo config beans and all property resolvers is loaded.
     *
     * @param referenceBean
     * @throws Exception
     */
    private synchronized void  initReferenceBean(ReferenceBean referenceBean) throws Exception {

        if (referenceBean.getReferenceConfig() != null) {
            return;
        }

        // reference key
        String referenceKey = ReferenceBeanSupport.generateReferenceKey(referenceBean, applicationContext.getEnvironment());

        ReferenceConfig referenceConfig = referenceConfigMap.get(referenceKey);
        if (referenceConfig == null) {
            //create real ReferenceConfig
            Map<String, Object> referenceAttributes = ReferenceBeanSupport.getReferenceAttributes(referenceBean);
            referenceConfig = ReferenceCreator.create(referenceAttributes, applicationContext)
                    .defaultInterfaceClass(referenceBean.getObjectType())
                    .build();
            //cache referenceConfig
            referenceConfigMap.put(referenceKey, referenceConfig);
            // register ReferenceConfig
            DubboBootstrap.getInstance().reference(referenceConfig);
        }

        // associate referenceConfig to referenceBean
        referenceBean.setKeyAndReferenceConfig(referenceKey, referenceConfig);
    }

}
