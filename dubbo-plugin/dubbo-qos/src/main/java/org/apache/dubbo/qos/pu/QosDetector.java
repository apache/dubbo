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

import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.rpc.model.FrameworkModel;

public class QosDetector implements ProtocolDetector {

    private final QosHTTP1Detector qosHTTP1Detector = new QosHTTP1Detector();
    private final TelnetDetector telnetDetector;
    private boolean QosEnableFlag = true;

    public void setQosEnableFlag(boolean qosEnableFlag) {
        QosEnableFlag = qosEnableFlag;
    }

    public QosDetector(FrameworkModel frameworkModel) {
        this.telnetDetector = new TelnetDetector(frameworkModel);
    }

    @Override
    public Result detect(ChannelBuffer in) {
        if(!QosEnableFlag) {
            return Result.UNRECOGNIZED;
        }
        Result h1Res = qosHTTP1Detector.detect(in);
        if(h1Res.equals(Result.RECOGNIZED)) {
            return h1Res;
        }
        Result telRes = telnetDetector.detect(in);
        if(telRes.equals(Result.RECOGNIZED)) {
            return telRes;
        }
        if(h1Res.equals(Result.NEED_MORE_DATA) || telRes.equals(Result.NEED_MORE_DATA)) {
            return Result.NEED_MORE_DATA;
        }
        return Result.UNRECOGNIZED;
    }

}
