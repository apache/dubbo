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
package org.apache.dubbo.remoting.utils;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;

public class PayloadDropper {
    private static Logger logger = LoggerFactory.getLogger(PayloadDropper.class);

    /**
     * only log body in debugger mode for size & security consideration.
     *
     * @param message
     * @return
     */
    public static Object getRequestWithoutData(Object message) {
        if (logger.isDebugEnabled()) {
            return message;
        }
        if (message instanceof Request) {
            Request request = (Request) message;
            request.setData(null);
            return request;
        } else if (message instanceof Response) {
            Response response = (Response) message;
            response.setResult(null);
            return response;
        }
        return message;
    }
}
