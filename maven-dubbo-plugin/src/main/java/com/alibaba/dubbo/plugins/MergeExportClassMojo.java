/*
 * Copyright 1999-2011 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.plexus.util.IOUtil;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 * @goal merge-export-class
 * @phase process-resources
 * @requiresDependencyResolution runtime
 */
public class MergeExportClassMojo extends AbstractMojo {

    private static final Pattern CLASS_SEPARATOR_PATTERN = Pattern.compile("\\s*,\\s*");

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /**
     * @parameter expression="${output.file.name}" default-value="hsf_bundle.properties"
     */
    private String outputFileName;

    /**
     * @parameter expression="${output.directory}" default-value="${project.build.outputDirectory}"
     */
    private File outputDirectory;

    /**
     * @parameter expression="${source.encoding}" default-value="${project.build.sourceEncoding}"
     */
    private String sourceEncoding;

    /**
     * @parameter expression="${export.class.skip}" default-value="false"
     */
    private boolean skip;

    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("Skip merge export class(es)");
            return;
        }

        Set artfaicts = project.getArtifacts();
        ClassWorld world = new ClassWorld();
        ClassRealm realm = null;
        try {
            realm = world.newRealm("dependencies", null);
        } catch (DuplicateRealmException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        for (Iterator iterator = artfaicts.iterator(); iterator.hasNext(); ) {
            try {
                realm.addConstituent(((Artifact) iterator.next()).getFile()
                                         .toURI().toURL());
            } catch (MalformedURLException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        Enumeration<URL> urls = null;
        try {
            urls = realm.getClassLoader().getResources(outputFileName);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        Set<String> classes = new HashSet<String>();
        Properties properties = null;
        if (urls != null) {
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                properties = new Properties();
                InputStream is = null;
                try {
                    is = url.openStream();
                    properties.load(is);
                } catch (IOException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                } finally {
                    IOUtil.close(is);
                }
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key).trim();
                    classes.addAll(Arrays.asList(
                        CLASS_SEPARATOR_PATTERN.split(value)));
                }
            }
        }

        File dest = new File(outputDirectory, outputFileName);
        if (!dest.getParentFile().isDirectory()) {
            dest.getParentFile().mkdirs();
        }
        if (!dest.isFile()) {
            try {
                dest.createNewFile();
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        if (!classes.isEmpty()) {
            GenerateExportClassMojo.save(project.getArtifactId(), classes, dest, getLog());
        }

    }

}

