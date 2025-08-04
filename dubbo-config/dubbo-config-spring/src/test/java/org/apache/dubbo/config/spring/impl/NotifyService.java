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
package org.apache.dubbo.config.spring.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifyService {
    private static final Logger logger = LoggerFactory.getLogger(NotifyService.class);

    public void onInvoke(Object[] params) {
        logger.info("invoke param-0: {}", params[0]);
    }

    public void onReturn(Object result, Object[] params) {
        logger.info("invoke param-0: {}, return: {}", params[0], result);
    }

    public void onThrow(Throwable t, Object[] params) {
        logger.info("invoke param-0: {}, throw: {}", params[0], t.getMessage());
    }
}
