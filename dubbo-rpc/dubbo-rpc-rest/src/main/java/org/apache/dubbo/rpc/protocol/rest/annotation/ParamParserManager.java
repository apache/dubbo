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
package org.apache.dubbo.rpc.protocol.rest.annotation;


import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.consumer.BaseConsumerParamParser;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.consumer.ConsumerParseContext;

import java.util.List;
import java.util.Set;

public class ParamParserManager {


    private static final Set<BaseConsumerParamParser> consumerParamParsers =
        FrameworkModel.defaultModel().getExtensionLoader(BaseConsumerParamParser.class).getSupportedExtensionInstances();

    /**
     * provider  Design Description:
     * <p>
     * Object[] args=new Object[0];
     * List<Object> argsList=new ArrayList<>;</>
     * <p>
     * setValueByIndex(int index,Object value);
     * <p>
     * args=toArray(new Object[0]);
     */
    public void consumerParamParse(ConsumerParseContext parseContext) {

        List<Object> args = parseContext.getArgs();

        for (int i = 0; i < args.size(); i++) {
            for (BaseConsumerParamParser paramParser : consumerParamParsers) {
                ArgInfo argInfoByIndex = parseContext.getArgInfoByIndex(i);

                if (!paramParser.paramTypeMatch(argInfoByIndex)) {
                    continue;
                }

                paramParser.parse(parseContext, argInfoByIndex);
            }
        }

    }
}
