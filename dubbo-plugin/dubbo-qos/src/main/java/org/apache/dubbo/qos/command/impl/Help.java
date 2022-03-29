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
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.qos.command.util.CommandHelper;
import org.apache.dubbo.qos.textui.TTable;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Cmd(name = "help", summary = "help command", example = {
        "help",
        "help online"
})
public class Help implements BaseCommand {

    private CommandHelper commandHelper;

    private static final String MAIN_HELP = "mainHelp";

    private static Map<String, String> processedTable = new WeakHashMap<>();

    public Help(FrameworkModel frameworkModel) {
        this.commandHelper = new CommandHelper(frameworkModel);
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        if (ArrayUtils.isNotEmpty(args)) {
            return processedTable.computeIfAbsent(args[0], commandName -> commandHelp(commandName));
        } else {
            return processedTable.computeIfAbsent(MAIN_HELP, commandName -> mainHelp());
        }
    }


    private String commandHelp(String commandName) {

        if (!commandHelper.hasCommand(commandName)) {
            return "no such command:" + commandName;
        }

        Class<?> clazz = commandHelper.getCommandClass(commandName);

        final Cmd cmd = clazz.getAnnotation(Cmd.class);
        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(80, false, TTable.Align.LEFT)
        });

        tTable.addRow("COMMAND NAME", commandName);

        if (null != cmd.example()) {
            tTable.addRow("EXAMPLE", drawExample(cmd));
        }

        return tTable.padding(1).rendering();
    }

    private String drawExample(Cmd cmd) {
        final StringBuilder drawExampleStringBuilder = new StringBuilder();
        for (String example : cmd.example()) {
            drawExampleStringBuilder.append(example).append('\n');
        }
        return drawExampleStringBuilder.toString();
    }

    /*
     * output main help
     */
    private String mainHelp() {

        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(80, false, TTable.Align.LEFT)
        });

        final List<Class<?>> classes = commandHelper.getAllCommandClass();

        Collections.sort(classes, new Comparator<Class<?>>() {

            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                final Integer o1s = o1.getAnnotation(Cmd.class).sort();
                final Integer o2s = o2.getAnnotation(Cmd.class).sort();
                return o1s.compareTo(o2s);
            }

        });
        for (Class<?> clazz : classes) {

            if (clazz.isAnnotationPresent(Cmd.class)) {
                final Cmd cmd = clazz.getAnnotation(Cmd.class);
                tTable.addRow(cmd.name(), cmd.summary());
            }

        }

        return tTable.padding(1).rendering();
    }
}
