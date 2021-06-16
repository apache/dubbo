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
package org.apache.dubbo.spring.boot.actuate.endpoint.mvc;

import org.apache.dubbo.spring.boot.actuate.endpoint.DubboEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboConfigsMetadata;
import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboPropertiesMetadata;
import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboReferencesMetadata;
import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboServicesMetadata;
import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboShutdownMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;
import java.util.SortedMap;

/**
 * {@link MvcEndpoint} to expose Dubbo Metadata
 *
 * @see MvcEndpoint
 * @since 2.7.0
 */
public class DubboMvcEndpoint extends EndpointMvcAdapter {

    public static final String DUBBO_SHUTDOWN_ENDPOINT_URI = "/shutdown";

    public static final String DUBBO_CONFIGS_ENDPOINT_URI = "/configs";

    public static final String DUBBO_SERVICES_ENDPOINT_URI = "/services";

    public static final String DUBBO_REFERENCES_ENDPOINT_URI = "/references";

    public static final String DUBBO_PROPERTIES_ENDPOINT_URI = "/properties";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DubboShutdownMetadata dubboShutdownMetadata;

    @Autowired
    private DubboConfigsMetadata dubboConfigsMetadata;

    @Autowired
    private DubboServicesMetadata dubboServicesMetadata;

    @Autowired
    private DubboReferencesMetadata dubboReferencesMetadata;

    @Autowired
    private DubboPropertiesMetadata dubboPropertiesMetadata;

    public DubboMvcEndpoint(DubboEndpoint dubboEndpoint) {
        super(dubboEndpoint);
    }


    @RequestMapping(value = DUBBO_SHUTDOWN_ENDPOINT_URI, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public DeferredResult shutdown() throws Exception {
        Map<String, Object> shutdownCountData = dubboShutdownMetadata.shutdown();
        return new DeferredResult(null, shutdownCountData);
    }

    @RequestMapping(value = DUBBO_CONFIGS_ENDPOINT_URI, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Map<String, Map<String, Object>>> configs() {
        return dubboConfigsMetadata.configs();
    }


    @RequestMapping(value = DUBBO_SERVICES_ENDPOINT_URI, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Map<String, Object>> services() {
        return dubboServicesMetadata.services();
    }

    @RequestMapping(value = DUBBO_REFERENCES_ENDPOINT_URI, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Map<String, Object>> references() {
        return dubboReferencesMetadata.references();
    }

    @RequestMapping(value = DUBBO_PROPERTIES_ENDPOINT_URI, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public SortedMap<String, Object> properties() {
        return dubboPropertiesMetadata.properties();

    }
}
