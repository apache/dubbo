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
package org.apache.dubbo.rpc.protocol.rest.pu;

import org.apache.dubbo.remoting.api.AbstractHttpProtocolDetector;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.rpc.model.FrameworkModel;

/**
 * rest http protocol detector
 */
public class RestHttp1Detector extends AbstractHttpProtocolDetector {
    private static final char[][] HTTP_METHODS_PREFIX = getHttpMethodsPrefix();

    private FrameworkModel frameworkModel;

    public RestHttp1Detector(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public Result detect(ChannelBuffer in) {

        int i = in.readableBytes();

        // length judge
        if (i < SIMPLE_HTTP.length()) {
            return Result.unrecognized();
        }

        if (prefixMatch(HTTP_METHODS_PREFIX, in, 3)) {
            return Result.recognized();
        }

        return Result.unrecognized();
    }
}
