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

import org.apache.maven.plugin.logging.Log;

public class WinDubboProtocPluginWrapper implements DubboProtocPluginWrapper {

    @Override
    public File createProtocPlugin(DubboProtocPlugin dubboProtocPlugin, Log log) {
        File pluginDirectory = dubboProtocPlugin.getPluginDirectory();
        pluginDirectory.mkdirs();
        if (!pluginDirectory.isDirectory()) {
            throw new RuntimeException(
                    "Unable to create protoc plugin directory: " + pluginDirectory.getAbsolutePath());
        }
        File batFile =
                new File(dubboProtocPlugin.getPluginDirectory(), "protoc-gen-" + dubboProtocPlugin.getId() + ".bat");

        try (PrintWriter out = new PrintWriter(new FileWriter(batFile))) {
            out.println("@echo off");
            out.println("set JAVA_HOME=" + dubboProtocPlugin.getJavaHome());
            StringBuilder classpath = new StringBuilder(256);
            classpath.append("set CLASSPATH=");
            for (File jar : dubboProtocPlugin.getResolvedJars()) {
                classpath.append(jar.getAbsolutePath()).append(";");
            }
            out.println(classpath);
            out.println("\"%JAVA_HOME%\\bin\\java\" ^");
            for (String jvmArg : dubboProtocPlugin.getJvmArgs()) {
                out.println("  " + jvmArg + " ^");
            }
            out.println("  " + dubboProtocPlugin.getMainClass() + " ^");
            for (String arg : dubboProtocPlugin.getArgs()) {
                out.println("  " + arg + " ^");
            }
            out.println("  %*");
        } catch (IOException e) {
            throw new RuntimeException("Unable to write BAT file: " + batFile.getAbsolutePath(), e);
        }

        return batFile;
    }
}
