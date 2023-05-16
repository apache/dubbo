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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_DUBBO_BEAN_INITIALIZER;


public class ReferenceBeanManager implements ApplicationContextAware {
    public static final String BEAN_NAME = "dubboReferenceBeanManager";
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    //reference key -> reference bean names
    private ConcurrentMap<String, List<String>> referenceKeyMap = new ConcurrentHashMap<>();

    // reference alias -> reference bean name
    private ConcurrentMap<String, String> referenceAliasMap = new ConcurrentHashMap<>();

    //reference bean name -> ReferenceBean
    private ConcurrentMap<String, ReferenceBean> referenceBeanMap = new ConcurrentHashMap<>();

    //reference key -> ReferenceConfig instance
    private ConcurrentMap<String, ReferenceConfig> referenceConfigMap = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;
    private volatile boolean initialized = false;
    private ModuleModel moduleModel;

    public void addReference(ReferenceBean referenceBean) throws Exception {
        String referenceBeanName = referenceBean.getId();
        Assert.notEmptyString(referenceBeanName, "The id of ReferenceBean cannot be empty");

        if (!initialized) {
            //TODO add issue url to describe early initialization
            logger.warn(CONFIG_DUBBO_BEAN_INITIALIZER, "", "", "Early initialize reference bean before DubboConfigBeanInitializer," +
                " the BeanPostProcessor has not been loaded at this time, which may cause abnormalities in some components (such as seata): " +
                referenceBeanName + " = " + ReferenceBeanSupport.generateReferenceKey(referenceBean, applicationContext));
        }
        String referenceKey = getReferenceKeyByBeanName(referenceBeanName);
        if (StringUtils.isEmpty(referenceKey)) {
            referenceKey = ReferenceBeanSupport.generateReferenceKey(referenceBean, applicationContext);
        }
        ReferenceBean oldReferenceBean = referenceBeanMap.get(referenceBeanName);
        if (oldReferenceBean != null) {
            if (referenceBean != oldReferenceBean) {
                String oldReferenceKey = ReferenceBeanSupport.generateReferenceKey(oldReferenceBean, applicationContext);
                throw new IllegalStateException("Found duplicated ReferenceBean with id: " + referenceBeanName +
                    ", old: " + oldReferenceKey + ", new: " + referenceKey);
            }
            return;
        }
        referenceBeanMap.put(referenceBeanName, referenceBean);
        //save cache, map reference key to referenceBeanName
        this.registerReferenceKeyAndBeanName(referenceKey, referenceBeanName);

        // if add reference after prepareReferenceBeans(), should init it immediately.
        if (initialized) {
            initReferenceBean(referenceBean);
        }
    }

    private String getReferenceKeyByBeanName(String referenceBeanName) {
        Set<Map.Entry<String, List<String>>> entries = referenceKeyMap.entrySet();
        for (Map.Entry<String, List<String>> entry : entries) {
            if (entry.getValue().contains(referenceBeanName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void registerReferenceKeyAndBeanName(String referenceKey, String referenceBeanNameOrAlias) {
        List<String> list = ConcurrentHashMapUtils.computeIfAbsent(referenceKeyMap, referenceKey, (key) -> new ArrayList<>());
        if (!list.contains(referenceBeanNameOrAlias)) {
            list.add(referenceBeanNameOrAlias);
            // register bean name as alias
            referenceAliasMap.put(referenceBeanNameOrAlias, list.get(0));
        }
    }

    public ReferenceBean getById(String referenceBeanNameOrAlias) {
        String referenceBeanName = transformName(referenceBeanNameOrAlias);
        return referenceBeanMap.get(referenceBeanName);
    }

    // convert reference name/alias to referenceBeanName
    private String transformName(String referenceBeanNameOrAlias) {
        return referenceAliasMap.getOrDefault(referenceBeanNameOrAlias, referenceBeanNameOrAlias);
    }

    public List<String> getBeanNamesByKey(String key) {
        return Collections.unmodifiableList(referenceKeyMap.getOrDefault(key, Collections.EMPTY_LIST));
    }

    public Collection<ReferenceBean> getReferences() {
        return new HashSet<>(referenceBeanMap.values());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        moduleModel = DubboBeanUtils.getModuleModel(applicationContext);
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
    }

    /**
     * NOTE: This method should only call after all dubbo config beans and all property resolvers is loaded.
     *
     * @param referenceBean
     * @throws Exception
     */
    private synchronized void initReferenceBean(ReferenceBean referenceBean) throws Exception {

        if (referenceBean.getReferenceConfig() != null) {
            return;
        }

        // TOTO check same unique service name but difference reference key (means difference attributes).

        // reference key
        String referenceKey = getReferenceKeyByBeanName(referenceBean.getId());
        if (StringUtils.isEmpty(referenceKey)) {
            referenceKey = ReferenceBeanSupport.generateReferenceKey(referenceBean, applicationContext);
        }

        ReferenceConfig referenceConfig = referenceConfigMap.get(referenceKey);
        if (referenceConfig == null) {
            //create real ReferenceConfig
            Map<String, Object> referenceAttributes = ReferenceBeanSupport.getReferenceAttributes(referenceBean);
            referenceConfig = ReferenceCreator.create(referenceAttributes, applicationContext)
                .defaultInterfaceClass(referenceBean.getObjectType())
                .build();

            // set id if it is not a generated name
            if (referenceBean.getId() != null && !referenceBean.getId().contains("#")) {
                referenceConfig.setId(referenceBean.getId());
            }

            //cache referenceConfig
            referenceConfigMap.put(referenceKey, referenceConfig);

            // register ReferenceConfig
            moduleModel.getConfigManager().addReference(referenceConfig);
        }

        // associate referenceConfig to referenceBean
        referenceBean.setKeyAndReferenceConfig(referenceKey, referenceConfig);
    }

}
