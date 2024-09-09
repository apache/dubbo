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
package org.apache.dubbo.maven.plugin.protoc;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class ProtocMetaData {

    private String protocExecutable;
    private Collection<File> protoSourceDirs;
    private List<File> protoFiles;
    private File outputDir;
    private DubboProtocPlugin dubboProtocPlugin;

    public ProtocMetaData() {}

    public ProtocMetaData(
            String protocExecutable,
            Collection<File> protoSourceDirs,
            List<File> protoFiles,
            File outputDir,
            DubboProtocPlugin dubboProtocPlugin) {
        this.protocExecutable = protocExecutable;
        this.protoSourceDirs = protoSourceDirs;
        this.protoFiles = protoFiles;
        this.outputDir = outputDir;
        this.dubboProtocPlugin = dubboProtocPlugin;
    }

    public String getProtocExecutable() {
        return protocExecutable;
    }

    public void setProtocExecutable(String protocExecutable) {
        this.protocExecutable = protocExecutable;
    }

    public Collection<File> getProtoSourceDirs() {
        return protoSourceDirs;
    }

    public void setProtoSourceDirs(Collection<File> protoSourceDirs) {
        this.protoSourceDirs = protoSourceDirs;
    }

    public List<File> getProtoFiles() {
        return protoFiles;
    }

    public void setProtoFiles(List<File> protoFiles) {
        this.protoFiles = protoFiles;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public DubboProtocPlugin getDubboProtocPlugin() {
        return dubboProtocPlugin;
    }

    public void setDubboProtocPlugin(DubboProtocPlugin dubboProtocPlugin) {
        this.dubboProtocPlugin = dubboProtocPlugin;
    }
}
