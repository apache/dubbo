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
package org.apache.dubbo.gen;

import java.io.IOException;
import java.util.List;

import com.google.protobuf.compiler.PluginProtos;

public class DubboGeneratorPlugin {

    public static void generate(AbstractGenerator generator) {
        try {
            PluginProtos.CodeGeneratorRequest request = PluginProtos.CodeGeneratorRequest.parseFrom(System.in);
            List<PluginProtos.CodeGeneratorResponse.File> files = generator.generateFiles(request);
            PluginProtos.CodeGeneratorResponse.newBuilder()
                    .addAllFile(files)
                    .setSupportedFeatures(
                            PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL.getNumber())
                    .build()
                    .writeTo(System.out);
        } catch (Exception e) {
            try {
                PluginProtos.CodeGeneratorResponse.newBuilder()
                        .setError(e.getMessage())
                        .build()
                        .writeTo(System.out);
            } catch (IOException var6) {
                exit(e);
            }
        } catch (Throwable var8) {
            exit(var8);
        }
    }

    public static void exit(Throwable e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }
}
