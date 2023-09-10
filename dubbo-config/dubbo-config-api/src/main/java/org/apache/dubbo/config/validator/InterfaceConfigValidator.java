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
package org.apache.dubbo.config.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.FILTER_KEY;
import static org.apache.dubbo.config.Constants.LAYER_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_KEY;
import static org.apache.dubbo.rpc.Constants.PROXY_KEY;

@Activate
public class InterfaceConfigValidator implements ConfigValidator<AbstractInterfaceConfig> {

    public static void validateAbstractInterfaceConfig(AbstractInterfaceConfig config) {
        ConfigValidationUtils.checkName(LOCAL_KEY, config.getLocal());
        ConfigValidationUtils.checkName("stub", config.getStub());
        ConfigValidationUtils.checkMultiName("owner", config.getOwner());

        ConfigValidationUtils.checkExtension(config.getScopeModel(), ProxyFactory.class, PROXY_KEY, config.getProxy());
        ConfigValidationUtils.checkExtension(config.getScopeModel(), Cluster.class, CLUSTER_KEY, config.getCluster());
        ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), Arrays.asList(Filter.class, ClusterFilter.class), FILTER_KEY, config.getFilter());
        ConfigValidationUtils.checkNameHasSymbol(LAYER_KEY, config.getLayer());

        List<MethodConfig> methods = config.getMethods();
        if (CollectionUtils.isNotEmpty(methods)) {
            methods.forEach(MethodConfig::validate);
        }
    }

    @Override
    public boolean validate(AbstractInterfaceConfig config) {
        validateAbstractInterfaceConfig(config);
        return true;
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return AbstractInterfaceConfig.class.isAssignableFrom(configClass);
    }
}
