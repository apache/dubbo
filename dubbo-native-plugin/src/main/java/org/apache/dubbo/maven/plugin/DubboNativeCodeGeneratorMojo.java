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

package org.apache.dubbo.maven.plugin;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * generate related self-adaptive code (native image does not support dynamic code generation. Therefore, code needs to be generated before compilation)
 */
@Mojo(name = "generate")
public class DubboNativeCodeGeneratorMojo extends AbstractMojo {

    @Override
    public void execute() {
        Log log = getLog();
        log.info("dubbo native code generator mojo execute");
        MavenProject project = (MavenProject) this.getPluginContext().get("project");
        copyNativeConfigFile(log, project);
        try {
            generateCode(log, project);
        } catch (Exception ignored) {

        }
    }

    private void generateCode(Log log, MavenProject project) throws IOException {
        String baseDir = project.getBasedir().getPath();
        File source = new File(baseDir + "/src/main/generated");
        FileUtils.forceMkdir(source);
        project.addCompileSourceRoot(source.getAbsolutePath());
        log.info("Source directory: " + source + " added.");
        List<String> list = project.getCompileSourceRoots();
        log.info(list.toString());
        CodeGenerator.execute(source.getPath(), log);
    }

    private void copyNativeConfigFile(Log log, MavenProject project) {
        String[] nativeFiles = {"META-INF/native-image/reflect-config.json",
            "META-INF/native-image/jni-config.json",
            "META-INF/native-image/proxy-config.json",
            "META-INF/native-image/resource-config.json",
            "META-INF/native-image/serialization-config.json"};

        Arrays.stream(nativeFiles).forEach(nativeFile -> {
            InputStream is = DubboNativeCodeGeneratorMojo.class.getClassLoader().getResourceAsStream(nativeFile);
            project.getResources().stream().findFirst().ifPresent(resource -> {
                String directory = resource.getDirectory();
                try {
                    FileUtils.forceMkdir(new File(directory + "/META-INF/native-image/"));
                    File file = new File(directory + "/" + nativeFile);
                    if (!file.exists()) {
                        FileUtils.copyInputStreamToFile(is, file);
                        log.info("Copy native config file:" + file);
                    } else {
                        log.info("Skip copy config file:" + file);
                    }
                } catch (Throwable ex) {
                    log.error("Copy native config file error:" + ex.getMessage());
                }
            });
        });
    }
}
