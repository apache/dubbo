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
package org.apache.dubbo.maven.plugin.protoc.command;

import org.apache.dubbo.maven.plugin.protoc.DubboProtocPlugin;
import org.apache.dubbo.maven.plugin.protoc.ProtocMetaData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DefaultProtocCommandBuilder implements ProtocCommandArgsBuilder {

    @Override
    public List<String> buildProtocCommandArgs(ProtocMetaData protocMetaData) {
        List<String> command = new ArrayList<>();
        for (final File protoSourceDir : protocMetaData.getProtoSourceDirs()) {
            command.add("--proto_path=" + protoSourceDir);
        }
        String outputOption = "--java_out=";
        outputOption += protocMetaData.getOutputDir();
        command.add(outputOption);
        DubboProtocPlugin dubboProtocPlugin = protocMetaData.getDubboProtocPlugin();
        command.add("--plugin=protoc-gen-" + dubboProtocPlugin.getId() + '=' + dubboProtocPlugin.getProtocPlugin());
        command.add("--" + dubboProtocPlugin.getId() + "_out=" + protocMetaData.getOutputDir());
        for (final File protoFile : protocMetaData.getProtoFiles()) {
            command.add(protoFile.toString());
        }
        return command;
    }
}
