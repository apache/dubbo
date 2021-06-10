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
package org.apache.dubbo.config.spring;

/**
 * Constants of dubbo spring config
 */
public interface Constants {

    /**
     * attributes of reference annotation
     */
    String REFERENCE_PROPS = "referenceProps";

    /**
     * Registration sources of the reference, may be xml file or annotation location
     */
    String REFERENCE_SOURCES = "referenceSources";

    /**
     * The name of an attribute that can be
     * {@link org.springframework.core.AttributeAccessor#setAttribute set} on a
     * {@link org.springframework.beans.factory.config.BeanDefinition} so that
     * factory beans can signal their object type when it can't be deduced from
     * the factory bean class.
     * <p/>
     * From FactoryBean.OBJECT_TYPE_ATTRIBUTE of Spring 5.2.
     */
    String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";

}
