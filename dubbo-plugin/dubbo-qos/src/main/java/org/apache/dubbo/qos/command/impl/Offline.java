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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.qos.command.annotation.Cmd;

@Cmd(name = "offline", summary = "offline dubbo", example = {
        "offline dubbo",
        "offline xx.xx.xxx.service"
})
public class Offline extends BaseOffline {
    private Logger logger = LoggerFactory.getLogger(Offline.class);

    private static OfflineInterface offlineInterface = new OfflineInterface();
    private static OfflineApp offlineApp = new OfflineApp();

    @Override
    protected boolean doExecute(String servicePattern) {
        boolean r1 = offlineInterface.offline(servicePattern);
        boolean r2 = offlineApp.offline(servicePattern);
        return r1 && r2;
    }

}
