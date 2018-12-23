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
package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configurator. (SPI, Prototype, ThreadSafe)
 *
 */
public interface Configurator extends Comparable<Configurator> {

    /**
     * get the configurator url.
     *
     * @return configurator url.
     */
    URL getUrl();

    /**
     * Configure the provider url.
     * O
     *
     * @param url - old rovider url.
     * @return new provider url.
     */
    URL configure(URL url);


    /**
     * Convert override urls to map for use when re-refer.
     * Send all rules every time, the urls will be reassembled and calculated
     *
     * @param urls Contract:
     *             </br>1.override://0.0.0.0/...( or override://ip:port...?anyhost=true)&para1=value1... means global rules (all of the providers take effect)
     *             </br>2.override://ip:port...?anyhost=false Special rules (only for a certain provider)
     *             </br>3.override:// rule is not supported... ,needs to be calculated by registry itself.
     *             </br>4.override://0.0.0.0/ without parameters means clearing the override
     * @return
     */
    static Optional<List<Configurator>> toConfigurators(List<URL> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            return Optional.empty();
        }

        ConfiguratorFactory configuratorFactory = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                .getAdaptiveExtension();

        List<Configurator> configurators = new ArrayList<Configurator>(urls.size());
        for (URL url : urls) {
            if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                configurators.clear();
                break;
            }
            Map<String, String> override = new HashMap<String, String>(url.getParameters());
            //The anyhost parameter of override may be added automatically, it can't change the judgement of changing url
            override.remove(Constants.ANYHOST_KEY);
            if (override.size() == 0) {
                configurators.clear();
                continue;
            }
            configurators.add(configuratorFactory.getConfigurator(url));
        }
        Collections.sort(configurators);
        return Optional.of(configurators);
    }
}
