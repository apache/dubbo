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
package org.apache.dubbo.rpc.protocol.dubbo.telnet;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.remoting.telnet.support.Help;

/**
 * ShutdownTelnetHandler
 */
@Activate
@Help(parameter = "[-t <milliseconds>]", summary = "Shutdown Dubbo Application.", detail = "Shutdown Dubbo Application.")
public class ShutdownTelnetHandler implements TelnetHandler {
    @Override
    public String telnet(Channel channel, String message) throws RemotingException {

        int sleepMilliseconds = 0;
        if (StringUtils.isNotEmpty(message)) {
            String[] parameters = message.split("\\s+");
            if (parameters.length == 2 && parameters[0].equals("-t") && StringUtils.isInteger(parameters[1])) {
                sleepMilliseconds = Integer.parseInt(parameters[1]);
            } else {
                return "Invalid parameter,please input like shutdown -t 10000";
            }
        }
        long start = System.currentTimeMillis();
        if (sleepMilliseconds > 0) {
            try {
                Thread.sleep(sleepMilliseconds);
            } catch (InterruptedException e) {
                return "Failed to invoke shutdown command, cause: " + e.getMessage();
            }
        }
        StringBuilder buf = new StringBuilder();
        DubboShutdownHook.getDubboShutdownHook().unregister();
        DubboShutdownHook.getDubboShutdownHook().doDestroy();
        long end = System.currentTimeMillis();
        buf.append("Application has shutdown successfully");
        buf.append("\r\nelapsed: ");
        buf.append(end - start);
        buf.append(" ms.");
        return buf.toString();
    }
}
