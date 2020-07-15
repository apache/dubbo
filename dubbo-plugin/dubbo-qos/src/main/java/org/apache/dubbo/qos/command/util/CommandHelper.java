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
package org.apache.dubbo.qos.command.util;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.qos.command.BaseCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommandHelper {

    private CommandHelper() {
    }

    public static boolean hasCommand(String commandName) {

        BaseCommand command = null;
        try {
            command = ExtensionLoader.getExtensionLoader(BaseCommand.class).getExtension(commandName);
        } catch (Throwable throwable) {
            return false;
        }

        return command != null;

    }

    public static List<Class<?>> getAllCommandClass() {
        final Set<String> commandList = ExtensionLoader.getExtensionLoader(BaseCommand.class).getSupportedExtensions();
        final List<Class<?>> classes = new ArrayList<Class<?>>();

        for (String commandName : commandList) {
            BaseCommand command = ExtensionLoader.getExtensionLoader(BaseCommand.class).getExtension(commandName);
            classes.add(command.getClass());
        }

        return classes;
    }


    public static Class<?> getCommandClass(String commandName) {
        if (hasCommand(commandName)) {
            return ExtensionLoader.getExtensionLoader(BaseCommand.class).getExtension(commandName).getClass();
        } else {
            return null;
        }
    }
}
