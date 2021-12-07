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

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.lang.reflect.Method;
import java.util.List;

@Cmd(name = "select", summary = "Select the index of the method you want to invoke", example = {
    "select [index]"
})
public class SelectTelnet implements BaseCommand {
    public static final AttributeKey<Boolean> SELECT_KEY = AttributeKey.valueOf("telnet.select");
    public static final AttributeKey<Method> SELECT_METHOD_KEY = AttributeKey.valueOf("telnet.select.method");

    private final InvokeTelnet invokeTelnet;

    public SelectTelnet(FrameworkModel frameworkModel) {
        this.invokeTelnet = new InvokeTelnet(frameworkModel);
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        if (ArrayUtils.isEmpty(args)) {
            return "Please input the index of the method you want to invoke, eg: \r\n select 1";
        }
        Channel channel = commandContext.getRemote();
        String message = args[0];
        List<Method> methodList = channel.attr(InvokeTelnet.INVOKE_METHOD_LIST_KEY).get();
        if (CollectionUtils.isEmpty(methodList)) {
            return "Please use the invoke command first.";
        }
        if (!StringUtils.isInteger(message) || Integer.parseInt(message) < 1 || Integer.parseInt(message) > methodList.size()) {
            return "Illegal index ,please input select 1~" + methodList.size();
        }
        Method method = methodList.get(Integer.parseInt(message) - 1);
        channel.attr(SELECT_METHOD_KEY).set(method);
        channel.attr(SELECT_KEY).set(Boolean.TRUE);
        String invokeMessage = channel.attr(InvokeTelnet.INVOKE_MESSAGE_KEY).get();
        return invokeTelnet.execute(commandContext, new String[]{invokeMessage});
    }
}
