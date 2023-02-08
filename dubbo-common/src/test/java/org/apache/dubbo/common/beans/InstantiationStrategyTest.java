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
package org.apache.dubbo.common.beans;


import org.apache.dubbo.common.beans.model.FooBeanWithApplicationModel;
import org.apache.dubbo.common.beans.model.FooBeanWithFrameworkModel;
import org.apache.dubbo.common.beans.model.FooBeanWithModuleModel;
import org.apache.dubbo.common.beans.model.FooBeanWithScopeModel;
import org.apache.dubbo.common.beans.model.FooBeanWithoutUniqueConstructors;
import org.apache.dubbo.common.beans.support.InstantiationStrategy;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelAccessor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InstantiationStrategyTest {

    private ScopeModelAccessor scopeModelAccessor = new ScopeModelAccessor() {
        @Override
        public ScopeModel getScopeModel() {
            return ApplicationModel.defaultModel().getDefaultModule();
        }
    };

    @Test
    void testCreateBeanWithScopeModelArgument() throws ReflectiveOperationException {
        InstantiationStrategy instantiationStrategy = new InstantiationStrategy(scopeModelAccessor);

        FooBeanWithFrameworkModel beanWithFrameworkModel = instantiationStrategy.instantiate(FooBeanWithFrameworkModel.class);
        Assertions.assertSame(scopeModelAccessor.getFrameworkModel(), beanWithFrameworkModel.getFrameworkModel());

        FooBeanWithApplicationModel beanWithApplicationModel = instantiationStrategy.instantiate(FooBeanWithApplicationModel.class);
        Assertions.assertSame(scopeModelAccessor.getApplicationModel(), beanWithApplicationModel.getApplicationModel());

        FooBeanWithModuleModel beanWithModuleModel = instantiationStrategy.instantiate(FooBeanWithModuleModel.class);
        Assertions.assertSame(scopeModelAccessor.getModuleModel(), beanWithModuleModel.getModuleModel());

        FooBeanWithScopeModel beanWithScopeModel = instantiationStrategy.instantiate(FooBeanWithScopeModel.class);
        Assertions.assertSame(scopeModelAccessor.getScopeModel(), beanWithScopeModel.getScopeModel());

        // test not unique matched constructors
        try {
            instantiationStrategy.instantiate(FooBeanWithoutUniqueConstructors.class);
            Assertions.fail("Expect throwing exception");
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("Expect only one but found 2 matched constructors"), StringUtils.toString(e));
        }

    }


}