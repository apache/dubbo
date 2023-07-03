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
package org.apache.dubbo.qos.pu;

import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.remoting.api.AbstractHttpProtocolDetector;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.rpc.model.FrameworkModel;


public class QosHTTP1Detector extends AbstractHttpProtocolDetector {
    private static final char[][] QOS_METHODS_PREFIX = getQOSHttpMethodsPrefix();

    FrameworkModel frameworkModel;

    public QosHTTP1Detector(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }


    @Override
    public Result detect(ChannelBuffer in) {
        if (in.readableBytes() < 2) {
            return Result.needMoreData();
        }

        if (prefixMatch(QOS_METHODS_PREFIX, in, 3)) {

            // make distinguish from rest ,read request url
            String requestURL = readRequestLine(in);

            // url split by / length judge
            if (!isQosRequestURL(requestURL)) {
                return Result.unrecognized();
            }

            // command exist judge, when /cmd  or /cmd/appName we prefer response by rest http
            // decrease the affect to user
            BaseCommand command = commandExist(requestURL);

            if (command == null) {
                return Result.unrecognized();
            }


            return Result.recognized();
        }

        return Result.unrecognized();
    }

    private BaseCommand commandExist(String requestURL) {
        BaseCommand command = null;
        try {
            String cmd = splitAndGetFirst(requestURL);
            command = frameworkModel.getExtensionLoader(BaseCommand.class).getExtension(cmd);
        } catch (Throwable throwable) {
            //can't find command
        }
        return command;
    }
}
