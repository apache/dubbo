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

import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.qos.command.util.ServiceCheckUtils;
import org.apache.dubbo.qos.textui.TTable;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.Collection;

@Cmd(name = "ls", summary = "ls service", example = {
        "ls"
})
public class Ls implements BaseCommand {
    @Override
    public String execute(CommandContext commandContext, String[] args) {
        StringBuilder result = new StringBuilder();
        result.append(listProvider());
        result.append(listConsumer());

        return result.toString();
    }

    public String listProvider() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("As Provider side:" + System.lineSeparator());
        Collection<ProviderModel> providerModelList = ApplicationModel.allProviderModels();

        TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.MIDDLE),
                new TTable.ColumnDefine(TTable.Align.MIDDLE)
        });

        //Header
        tTable.addRow("Provider Service Name", "PUB");

        //Content
        for (ProviderModel providerModel : providerModelList) {
            tTable.addRow(providerModel.getServiceKey(), ServiceCheckUtils.isRegistered(providerModel) ? "Y" : "N");
        }
        stringBuilder.append(tTable.rendering());

        return stringBuilder.toString();
    }

    public String listConsumer() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("As Consumer side:" + System.lineSeparator());
        Collection<ConsumerModel> consumerModelList = ApplicationModel.allConsumerModels();

        TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.MIDDLE),
                new TTable.ColumnDefine(TTable.Align.MIDDLE)
        });

        //Header
        tTable.addRow("Consumer Service Name", "NUM");

        //Content
        //TODO to calculate consumerAddressNum
        for (ConsumerModel consumerModel : consumerModelList) {
            tTable.addRow(consumerModel.getServiceKey(), ServiceCheckUtils.getConsumerAddressNum(consumerModel));
        }

        stringBuilder.append(tTable.rendering());

        return stringBuilder.toString();
    }
}
