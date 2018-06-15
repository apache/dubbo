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
package org.apache.dubbo.qos.command;

import io.netty.channel.Channel;

public class CommandContext {
    private String commandName;
    private String[] args;
    private Channel remote;
    private boolean isHttp;
    private Object orginRequest;

    public CommandContext(String commandName) {
        this.commandName = commandName;
    }

    public CommandContext(String commandName, String[] args,boolean isHttp) {
        this.commandName = commandName;
        this.args = args;
        this.isHttp = isHttp;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public Channel getRemote() {
        return remote;
    }

    public void setRemote(Channel remote) {
        this.remote = remote;
    }

    public boolean isHttp() {
        return isHttp;
    }

    public void setHttp(boolean http) {
        isHttp = http;
    }

    public Object getOrginRequest() {
        return orginRequest;
    }

    public void setOrginRequest(Object orginRequest) {
        this.orginRequest = orginRequest;
    }
}
