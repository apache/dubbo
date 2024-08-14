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
package org.apache.dubbo.rpc.protocol.tri.transport;

import org.apache.dubbo.common.ServiceKey;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.rpc.executor.AbstractIsolationExecutorSupport;
import org.apache.dubbo.rpc.protocol.tri.RequestPath;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

public class TripleIsolationExecutorSupport extends AbstractIsolationExecutorSupport {
    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(TripleIsolationExecutorSupport.class);

    public TripleIsolationExecutorSupport(URL url) {
        super(url);
    }

    @Override
    protected ServiceKey getServiceKey(Object data) {
        if (!(data instanceof RequestMetadata)) {
            return null;
        }

        RequestMetadata httpMetadata = (RequestMetadata) data;
        RequestPath path = RequestPath.parse(httpMetadata.path());
        if (path == null) {
            return null;
        }

        HttpHeaders headers = httpMetadata.headers();
        return new ServiceKey(
                path.getServiceInterface(),
                getHeader(headers, TripleHeaderEnum.SERVICE_VERSION, RestConstants.HEADER_SERVICE_VERSION),
                getHeader(headers, TripleHeaderEnum.SERVICE_GROUP, RestConstants.HEADER_SERVICE_GROUP));
    }

    private static String getHeader(HttpHeaders headers, TripleHeaderEnum en, String key) {
        String value = headers.getFirst(en.getHeader());
        if (value == null) {
            value = headers.getFirst(key);
        }
        return value;
    }
}
