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
package org.apache.dubbo.remoting.http.ssl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.SslConfig;


/**
 * for rest client ssl context build
 */
public class RestClientSSLContexts {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RestClientSSLContexts.class);


    public static <T> T buildClientSSLContext(URL url, RestClientSSLContextSetter restClientSSLSetter, T restClient) {

        try {


           SslConfig  sslConfig = url.getOrDefaultApplicationModel().getApplicationConfigManager().getSsl().orElse(null);

            if (sslConfig == null) {
                return restClient;
            }


            if (sslConfig.isPem()) {
                return new PemSSLContextFactory().buildClientSSLContext(url, restClientSSLSetter, restClient);

            } else {
                return new JdkSSLContextFactory().buildClientSSLContext(url, restClientSSLSetter, restClient);
            }

        } catch (Throwable e) {
            logger.warn("", e.getMessage(), "", "Rest client build ssl context failed", e);

        }

        return restClient;

    }


}
