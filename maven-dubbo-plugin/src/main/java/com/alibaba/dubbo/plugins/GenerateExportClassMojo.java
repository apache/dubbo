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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 * @goal export-class
 * @phase generate-resources
 */
public class GenerateExportClassMojo extends AbstractMojo {

    private static final String LINE_SEPARATOR   = System.getProperty("line.separator");

    public static final  String EXPORT_CLASS_TAG = "export";

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
            getLog().info("Skip generate export class(es)");
            return;
        }

        JavaClass[] classes = scanSource();
        Set<String> set = new HashSet<String>();
        StringBuilder buf = new StringBuilder(" ");
        if (classes != null && classes.length > 0) {
            for (JavaClass cl : classes) {
                DocletTag tag = cl.getTagByName(EXPORT_CLASS_TAG);
                if (tag != null) {
                    set.add(cl.getFullyQualifiedName());
                }
            }
        }

        if (!set.isEmpty()) {
            File dest = new File(outputDirectory, outputFileName);
            save(project.getArtifactId(), set, dest, getLog());
        }

    }

    private JavaClass[] scanSource() {
        JavaDocBuilder builder = new JavaDocBuilder();
        String encoding = sourceEncoding;
        if (StringUtils.isEmpty(encoding)) {
            encoding = System.getProperty("file.encoding");
            getLog().warn(String.format("Using platform encoding (%s actually)" +
                                            " to read java source file, i.e. build is platform dependent!", encoding));
        } else {
            getLog().info(String.format("Using '%s' encoding to read java source file.", encoding));
        }

        for (Iterator iterator = project.getCompileSourceRoots().iterator(); iterator.hasNext(); ) {
            builder.addSourceTree(new File(iterator.next().toString()));
        }

        return builder.getClasses();
    }

    static void save(String key, Set<String> value, File dest, Log log) throws MojoExecutionException {
        StringBuilder buf = new StringBuilder(" ");
        for (String cl : value) {
            buf.append(cl).append(",");
        }
        buf.setLength(buf.length() - 1);
        if (buf.length() > 0) {
            Properties properties = new Properties();
            properties.setProperty(key, buf.toString().trim());

            FileOutputStream fos = null;
            try {
                if (!dest.getParentFile().isDirectory()) {
                    dest.getParentFile().mkdirs();
                }
                if (!dest.isFile()) {
                    dest.createNewFile();
                }
                fos = new FileOutputStream(dest);
                properties.store(fos, "Written by maven plugin");
                fos.close();
                log.info("Export class(es) in file " + dest.getAbsolutePath());
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            } finally {
                IOUtil.close(fos);
            }
        }
    }

}
