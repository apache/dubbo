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
import java.net.URL;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;

public class WinDubboProtocPluginWrapper implements DubboProtocPluginWrapper {

    private static final String DATA_MODEL_SYSPROP = "sun.arch.data.model";

    private static final String WIN_JVM_DATA_MODEL_32 = "32";

    private static final String WIN_JVM_DATA_MODEL_64 = "64";

    private String winJvmDataModel;

    private static File findJvmLocation(final File javaHome, final String... paths) {
        for (final String path : paths) {
            final File jvmLocation = new File(javaHome, path);
            if (jvmLocation.isFile()) {
                return jvmLocation;
            }
        }
        return null;
    }

    @Override
    public File createProtocPlugin(DubboProtocPlugin dubboProtocPlugin, Log log) {
        createPluginDirectory(dubboProtocPlugin.getPluginDirectory());
        final File javaHome = new File(dubboProtocPlugin.getJavaHome());
        final File jvmLocation = findJvmLocation(
                javaHome,
                "jre/bin/server/jvm.dll",
                "bin/server/jvm.dll",
                "jre/bin/client/jvm.dll",
                "bin/client/jvm.dll");
        final File winRun4JIniFile =
                new File(dubboProtocPlugin.getPluginDirectory(), dubboProtocPlugin.getId() + ".ini");

        if (winJvmDataModel != null) {
            if (!(winJvmDataModel.equals(WIN_JVM_DATA_MODEL_32) || winJvmDataModel.equals(WIN_JVM_DATA_MODEL_64))) {
                throw new RuntimeException("winJvmDataModel must be '32' or '64'");
            }
        } else if (archDirectoryExists("amd64", dubboProtocPlugin.getJavaHome())) {
            winJvmDataModel = WIN_JVM_DATA_MODEL_64;
            if (log.isDebugEnabled()) {
                log.debug("detected 64-bit JVM from directory structure");
            }
        } else if (archDirectoryExists("i386", dubboProtocPlugin.getJavaHome())) {
            winJvmDataModel = WIN_JVM_DATA_MODEL_32;
            if (log.isDebugEnabled()) {
                log.debug("detected 32-bit JVM from directory structure");
            }
        } else if (System.getProperty(DATA_MODEL_SYSPROP) != null) {
            winJvmDataModel = System.getProperty(DATA_MODEL_SYSPROP);
            if (log.isDebugEnabled()) {
                log.debug("detected " + winJvmDataModel + "-bit JVM from system property " + DATA_MODEL_SYSPROP);
            }
        } else {
            winJvmDataModel = WIN_JVM_DATA_MODEL_32;
            if (log.isDebugEnabled()) {
                log.debug("defaulting to 32-bit JVM");
            }
        }
        try (final PrintWriter out = new PrintWriter(new FileWriter(winRun4JIniFile))) {
            if (jvmLocation != null) {
                out.println("vm.location=" + jvmLocation.getAbsolutePath());
            }
            int index = 1;
            for (final File resolvedJar : dubboProtocPlugin.getResolvedJars()) {
                out.println("classpath." + index + "=" + resolvedJar.getAbsolutePath());
                index++;
            }
            out.println("main.class=" + dubboProtocPlugin.getMainClass());

            index = 1;
            for (final String arg : dubboProtocPlugin.getArgs()) {
                out.println("arg." + index + "=" + arg);
                index++;
            }

            index = 1;
            for (final String jvmArg : dubboProtocPlugin.getJvmArgs()) {
                out.println("vmarg." + index + "=" + jvmArg);
                index++;
            }

            out.println("vm.version.min=1.8");
            out.println("log.level=none");
            out.println("[ErrorMessages]");
            out.println("show.popup=false");
        } catch (IOException e) {
            throw new RuntimeException("Could not write WinRun4J ini file: " + winRun4JIniFile.getAbsolutePath(), e);
        }
        final String executablePath = getWinrun4jExecutablePath();
        final URL url = Thread.currentThread().getContextClassLoader().getResource(executablePath);
        if (url == null) {
            throw new RuntimeException("Could not locate WinRun4J executable at path: " + executablePath);
        }
        File pluginExecutableFile = getPluginExecutableFile(dubboProtocPlugin);
        try {
            FileUtils.copyURLToFile(url, pluginExecutableFile);
            return pluginExecutableFile;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not copy WinRun4J executable to: " + pluginExecutableFile.getAbsolutePath(), e);
        }
    }

    private void createPluginDirectory(File pluginDirectory) {
        pluginDirectory.mkdirs();
        if (!pluginDirectory.isDirectory()) {
            throw new RuntimeException(
                    "Could not create protoc plugin directory: " + pluginDirectory.getAbsolutePath());
        }
    }

    private boolean archDirectoryExists(String arch, String javaHome) {
        return javaHome != null
                && (new File(javaHome, "jre/lib/" + arch).isDirectory()
                        || new File(javaHome, "lib/" + arch).isDirectory());
    }

    private String getWinrun4jExecutablePath() {
        return "winrun4j/WinRun4J" + winJvmDataModel + ".exe";
    }

    public File getPluginExecutableFile(DubboProtocPlugin dubboProtocPlugin) {
        return new File(dubboProtocPlugin.getPluginDirectory(), "protoc-gen-" + dubboProtocPlugin.getId() + ".exe");
    }
}
