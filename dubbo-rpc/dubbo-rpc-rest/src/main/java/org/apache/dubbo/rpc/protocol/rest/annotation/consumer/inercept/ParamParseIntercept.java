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
package org.apache.dubbo.rpc.protocol.rest.annotation.consumer.inercept;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParamParserManager;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.consumer.ConsumerParseContext;
@Activate("paramparse")
public class ParamParseIntercept implements HttpConnectionPreBuildIntercept {
    private static final ParamParserManager paramParser = new ParamParserManager();

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {

        ConsumerParseContext consumerParseContext = new ConsumerParseContext();
        consumerParseContext.setArgInfos(connectionCreateContext.getRestMethodMetadata().getArgInfos());
        consumerParseContext.setArgs(connectionCreateContext.getMethodRealArgs());
        paramParser.consumerParamParse(consumerParseContext);
    }
}
