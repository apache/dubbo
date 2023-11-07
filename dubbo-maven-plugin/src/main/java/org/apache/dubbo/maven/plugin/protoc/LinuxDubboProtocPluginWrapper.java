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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class LinuxDubboProtocPluginWrapper implements DubboProtocPluginWrapper {

    @Override
    public File createProtocPlugin(DubboProtocPlugin dubboProtocPlugin, Log log) {
        List<File> resolvedJars = dubboProtocPlugin.getResolvedJars();
        createPluginDirectory(dubboProtocPlugin.getPluginDirectory());
        File pluginExecutableFile = new File(dubboProtocPlugin.getPluginDirectory(), dubboProtocPlugin.getPluginName());
        final File javaLocation = new File(dubboProtocPlugin.getJavaHome(), "bin/java");

        if (log.isDebugEnabled()) {
            log.debug("javaLocation=" + javaLocation.getAbsolutePath());
        }
        try (final PrintWriter out = new PrintWriter(new FileWriter(pluginExecutableFile))) {
            out.println("#!/bin/sh");
            out.println();
            out.print("CP=");
            for (int i = 0; i < resolvedJars.size(); i++) {
                if (i > 0) {
                    out.print(":");
                }
                out.print("\"" + resolvedJars.get(i).getAbsolutePath() + "\"");
            }
            out.println();
            out.print("ARGS=\"");
            for (final String arg : dubboProtocPlugin.getArgs()) {
                out.print(arg + " ");
            }
            out.println("\"");
            out.print("JVMARGS=\"");
            for (final String jvmArg : dubboProtocPlugin.getJvmArgs()) {
                out.print(jvmArg + " ");
            }
            out.println("\"");
            out.println();
            out.println("\"" + javaLocation.getAbsolutePath() + "\" $JVMARGS -cp $CP "
                    + dubboProtocPlugin.getMainClass() + " $ARGS");
            out.println();
            boolean b = pluginExecutableFile.setExecutable(true);
            if (!b) {
                throw new RuntimeException("Could not make plugin executable: " + pluginExecutableFile);
            }
            return pluginExecutableFile;
        } catch (IOException e) {
            throw new RuntimeException("Could not write plugin script file: " + pluginExecutableFile, e);
        }
    }

    private void createPluginDirectory(File pluginDirectory) {
        pluginDirectory.mkdirs();
        if (!pluginDirectory.isDirectory()) {
            throw new RuntimeException(
                    "Could not create protoc plugin directory: " + pluginDirectory.getAbsolutePath());
        }
    }
}
