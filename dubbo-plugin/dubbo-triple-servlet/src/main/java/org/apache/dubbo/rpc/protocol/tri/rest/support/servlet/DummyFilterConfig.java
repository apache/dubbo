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
package org.apache.dubbo.rpc.protocol.tri.rest.support.servlet;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import java.util.Collections;
import java.util.Enumeration;

final class DummyFilterConfig implements FilterConfig {

    private final String filterName;
    private final FrameworkModel frameworkModel;
    private final ServletContext servletContext;

    public DummyFilterConfig(String filterName, FrameworkModel frameworkModel, ServletContext servletContext) {
        this.filterName = filterName;
        this.frameworkModel = frameworkModel;
        this.servletContext = servletContext;
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        String prefix = RestConstants.CONFIG_PREFIX + "filter-config.";
        Configuration conf = ConfigurationUtils.getGlobalConfiguration(frameworkModel.defaultApplication());
        String value = conf.getString(prefix + filterName + "." + name);
        if (value == null) {
            value = conf.getString(prefix + name);
        }
        return value;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.emptyEnumeration();
    }
}
