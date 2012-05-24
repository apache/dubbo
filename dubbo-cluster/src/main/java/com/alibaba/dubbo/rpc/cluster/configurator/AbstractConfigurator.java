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
package com.alibaba.dubbo.rpc.cluster.configurator;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.cluster.Configurator;

/**
 * AbstractOverrideConfigurator
 * 
 * @author william.liangf
 */
public abstract class AbstractConfigurator implements Configurator {
    
    private final URL configuratorUrl;

    public AbstractConfigurator(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("configurator url == null");
        }
        this.configuratorUrl = url;
    }

    public URL getUrl() {
        return configuratorUrl;
    }

    public URL configure(URL url) {
        if (isMatch(getUrl(), url)) {
            return doConfigure(url);
        }
        return url;
    }

    public int compareTo(Configurator o) {
        if (o == null) {
            return -1;
        }
        return getUrl().getHost().compareTo(o.getUrl().getHost());
    }

    private boolean isMatch(URL configuratorUrl, URL providerUrl) {
        if (configuratorUrl == null || configuratorUrl.getHost() == null
                || providerUrl == null || providerUrl.getHost() == null) {
            return false;
        }
        /*if (! providerUrl.getServiceKey().equals(configuratorUrl.getServiceKey())) {
            return false;
        }*/
        if (Constants.ANYHOST_VALUE.equals(configuratorUrl.getHost()) 
                || providerUrl.getHost().equals(configuratorUrl.getHost())) {
            String configApplication = configuratorUrl.getParameter(Constants.APPLICATION_KEY, configuratorUrl.getUsername());
            String providerApplication = providerUrl.getParameter(Constants.APPLICATION_KEY, providerUrl.getUsername());
            if (configApplication == null || configApplication.equals(providerApplication)) {
                if (configuratorUrl.getPort() > 0) {
                    return providerUrl.getPort() == configuratorUrl.getPort();
                } else {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected abstract URL doConfigure(URL url);

}
