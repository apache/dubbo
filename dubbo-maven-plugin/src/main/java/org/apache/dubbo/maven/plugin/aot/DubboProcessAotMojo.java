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
package org.apache.dubbo.maven.plugin.aot;


import org.apache.commons.io.FileUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "dubbo-process-aot", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DubboProcessAotMojo extends AbstractAotMojo {

    private static final String AOT_PROCESSOR_CLASS_NAME = "org.apache.dubbo.aot.generate.CodeGenerator";

    /**
     * Directory containing the classes and resource files that should be packaged into
     * the archive.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;

    /**
     * Directory containing the generated sources.
     */
    @Parameter(defaultValue = "${project.build.directory}/dubbo-aot/main/sources", required = true)
    private File generatedSources;

    /**
     * Directory containing the generated resources.
     */
    @Parameter(defaultValue = "${project.build.directory}/dubbo-aot/main/resources", required = true)
    private File generatedResources;

    /**
     * Directory containing the generated classes.
     */
    @Parameter(defaultValue = "${project.build.directory}/dubbo-aot/main/classes", required = true)
    private File generatedClasses;

    /**
     * Name of the main class to use as the source for the AOT process. If not specified
     * the first compiled class found that contains a 'main' method will be used.
     */
    @Parameter(property = "dubbo.aot.main-class")
    private String mainClass;


    /**
     * Application arguments that should be taken into account for AOT processing.
     */
    @Parameter
    private String[] arguments;


    @Override
    protected void executeAot() throws Exception {
        URL[] classPath = getClassPath().toArray(new URL[0]);
        generateAotAssets(classPath, AOT_PROCESSOR_CLASS_NAME, getAotArguments(mainClass));
        compileSourceFiles(classPath, this.generatedSources, this.classesDirectory);
        copyNativeConfigFile(project);
        copyAll(this.generatedResources.toPath(), this.classesDirectory.toPath());
        copyAll(this.generatedClasses.toPath(), this.classesDirectory.toPath());
    }

    private String[] getAotArguments(String applicationClass) {
        List<String> aotArguments = new ArrayList<>();
        aotArguments.add(applicationClass);
        aotArguments.add(this.generatedSources.toString());
        aotArguments.add(this.generatedResources.toString());
        aotArguments.add(this.generatedClasses.toString());
        aotArguments.add(this.project.getGroupId());
        aotArguments.add(this.project.getArtifactId());
        aotArguments.addAll(new RunArguments(this.arguments).getArgs());
        return aotArguments.toArray(new String[0]);
    }

    private List<URL> getClassPath() throws Exception {
        File[] directories = new File[]{this.classesDirectory, this.generatedClasses};
        return getClassPath(directories, new ExcludeTestScopeArtifactFilter());
    }

    private void copyNativeConfigFile(MavenProject project) {
        String[] nativeFiles = {"META-INF/native-image/reflect-config.json",
            "META-INF/native-image/jni-config.json",
            "META-INF/native-image/proxy-config.json",
            "META-INF/native-image/resource-config.json",
            "META-INF/native-image/serialization-config.json"};

        Arrays.stream(nativeFiles).forEach(nativeFile -> {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(nativeFile);
            project.getResources().stream().findFirst().ifPresent(resource -> {
                try {
                    String path = generatedResources + File.separator + "META-INF" + File.separator + "native-image" + File.separator
                        + File.separator + this.project.getGroupId() + File.separator + this.project.getArtifactId();
                    FileUtils.forceMkdir(new File(path));
                    String[] str = nativeFile.split("/");
                    File file = new File(path + File.separator + str[str.length - 1]);
                    if (!file.exists()) {
                        FileUtils.copyInputStreamToFile(is, file);
                        getLog().info("Copy native config file:" + file);
                    } else {
                        getLog().info("Skip copy config file:" + file);
                    }
                } catch (Throwable ex) {
                    getLog().error("Copy native config file error:" + ex.getMessage());
                }
            });
        });
    }


}
