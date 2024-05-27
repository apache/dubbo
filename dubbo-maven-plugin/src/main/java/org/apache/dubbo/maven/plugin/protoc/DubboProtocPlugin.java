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
import java.util.ArrayList;
import java.util.List;

public class DubboProtocPlugin {

    private String id;
    private String mainClass;
    private String dubboVersion;
    private String javaHome;
    private File pluginDirectory;
    private List<File> resolvedJars = new ArrayList<>();
    private List<String> args = new ArrayList<>();
    private List<String> jvmArgs = new ArrayList<>();
    private File protocPlugin = null;

    public DubboProtocPlugin() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getDubboVersion() {
        return dubboVersion;
    }

    public void setDubboVersion(String dubboVersion) {
        this.dubboVersion = dubboVersion;
    }

    public String getJavaHome() {
        return javaHome;
    }

    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    public File getPluginDirectory() {
        return pluginDirectory;
    }

    public void setPluginDirectory(File pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
    }

    public List<File> getResolvedJars() {
        return resolvedJars;
    }

    public void setResolvedJars(List<File> resolvedJars) {
        this.resolvedJars = resolvedJars;
    }

    public void addResolvedJar(File jar) {
        resolvedJars.add(jar);
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public void addArg(String arg) {
        args.add(arg);
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public void addJvmArg(String jvmArg) {
        jvmArgs.add(jvmArg);
    }

    public String getPluginName() {
        return "protoc-gen-" + id;
    }

    public File getProtocPlugin() {
        return protocPlugin;
    }

    public void setProtocPlugin(File protocPlugin) {
        this.protocPlugin = protocPlugin;
    }

    @Override
    public String toString() {
        return "DubboProtocPlugin{" + "id='"
                + id + '\'' + ", mainClass='"
                + mainClass + '\'' + ", dubboVersion='"
                + dubboVersion + '\'' + ", javaHome='"
                + javaHome + '\'' + ", pluginDirectory="
                + pluginDirectory + ", resolvedJars="
                + resolvedJars + ", args="
                + args + ", jvmArgs="
                + jvmArgs + ", protocPlugin="
                + protocPlugin + '}';
    }
}
