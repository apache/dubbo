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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.PathUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static java.lang.String.format;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_SERVICE_NAME_MAPPING_PROPERTIES_PATH;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SERVICE_NAME_MAPPING_PROPERTIES_FILE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.utils.StringUtils.SLASH;
import static org.apache.dubbo.metadata.MetadataConstants.KEY_SEPARATOR;

/**
 * The externalized {@link Properties} file implementation of {@link ServiceNameMapping},
 * the default properties class path is
 * {@link CommonConstants#DEFAULT_SERVICE_NAME_MAPPING_PROPERTIES_PATH "/META-INF/dubbo/service-name-mapping.properties"},
 * whose format as following:
 * <pre>
 * dubbo\:com.acme.Interface1\:default = Service1
 * thirft\:com.acme.InterfaceX = Service1,Service2
 * rest\:com.acme.interfaceN = Service3
 * </pre>
 * <p>
 * THe search path could be configured by the externalized property {@link CommonConstants#SERVICE_NAME_MAPPING_PROPERTIES_FILE_KEY}
 *
 * @see ReadOnlyServiceNameMapping
 * @see ParameterizedServiceNameMapping
 * @since 2.7.8
 */
public class PropertiesFileServiceNameMapping extends ReadOnlyServiceNameMapping {

    /**
     * The priority of {@link PropertiesFileServiceNameMapping} is
     * lower than {@link ParameterizedServiceNameMapping}
     */
    static final int PRIORITY = ParameterizedServiceNameMapping.PRIORITY + 1;


    private final List<Properties> propertiesList;

    public PropertiesFileServiceNameMapping() {
        this.propertiesList = loadPropertiesList();
    }

    @Override
    public Set<String> get(URL subscribedURL) {
        String propertyKey = getPropertyKey(subscribedURL);
        String propertyValue = null;

        for (Properties properties : propertiesList) {
            propertyValue = properties.getProperty(propertyKey);
            if (propertyValue != null) {
                break;
            }
        }

        return getValue(propertyValue);
    }

    private String getPropertyKey(URL url) {
        String protocol = url.getProtocol();
        String serviceInterface = url.getServiceInterface();
        // Optional
        String group = url.getParameter(GROUP_KEY);
        String version = url.getParameter(VERSION_KEY);

        StringBuilder propertyKeyBuilder = new StringBuilder(protocol)
                .append(KEY_SEPARATOR)
                .append(serviceInterface);

        appendIfPresent(propertyKeyBuilder, group);
        appendIfPresent(propertyKeyBuilder, version);

        return propertyKeyBuilder.toString();
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        if (!StringUtils.isBlank(value)) {
            builder.append(KEY_SEPARATOR).append(value);
        }
    }

    private List<Properties> loadPropertiesList() {
        List<Properties> propertiesList = new LinkedList<>();
        String propertiesPath = getPropertiesPath();
        try {
            Enumeration<java.net.URL> resources = ClassUtils.getClassLoader().getResources(propertiesPath);
            while (resources.hasMoreElements()) {
                java.net.URL resource = resources.nextElement();
                InputStream inputStream = resource.openStream();
                Properties properties = new Properties();
                properties.load(new InputStreamReader(inputStream, "UTF-8"));
                propertiesList.add(properties);
            }
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(format("The path of ServiceNameMapping's Properties file[path : %s] can't be loaded", propertiesPath), e);
            }
        }
        return propertiesList;
    }

    private String getPropertiesPath() {
        Configuration configuration = ApplicationModel.getEnvironment().getConfiguration();
        String propertyPath = configuration.getString(SERVICE_NAME_MAPPING_PROPERTIES_FILE_KEY, DEFAULT_SERVICE_NAME_MAPPING_PROPERTIES_PATH);
        propertyPath = PathUtils.normalize(propertyPath);
        if (propertyPath.startsWith(SLASH)) {
            propertyPath = propertyPath.substring(SLASH.length());
        }
        return propertyPath;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
