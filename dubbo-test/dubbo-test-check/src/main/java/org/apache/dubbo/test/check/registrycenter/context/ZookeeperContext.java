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
package org.apache.dubbo.test.check.registrycenter.context;

import org.apache.dubbo.test.check.registrycenter.Context;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperConfig;

import java.nio.file.Path;

/**
 * The global context for zookeeper.
 */
public class ZookeeperContext implements Context {

    /**
     * The config of zookeeper.
     */
    private ZookeeperConfig config = new ZookeeperConfig();

    /**
     * The the source file path of downloaded zookeeper binary archive.
     */
    private Path sourceFile;

    /**
     * The directory after unpacked zookeeper archive binary file.
     */
    private String unpackedDirectory;

    /**
     * Sets the source file path of downloaded zookeeper binary archive.
     */
    public void setSourceFile(Path sourceFile) {
        this.sourceFile = sourceFile;
    }

    /**
     * Returns the source file path of downloaded zookeeper binary archive.
     */
    public Path getSourceFile() {
        return this.sourceFile;
    }

    /**
     * Returns the directory after unpacked zookeeper archive binary file.
     */
    public String getUnpackedDirectory() {
        return unpackedDirectory;
    }

    /**
     * Sets the directory after unpacked zookeeper archive binary file.
     */
    public void setUnpackedDirectory(String unpackedDirectory) {
        this.unpackedDirectory = unpackedDirectory;
    }

    /**
     * Returns the zookeeper's version.
     */
    public String getVersion() {
        return config.getVersion();
    }

    /**
     * Returns the client ports of zookeeper.
     */
    public int[] getClientPorts() {
        return config.getClientPorts();
    }

    /**
     * Returns the admin server ports of zookeeper.
     */
    public int[] getAdminServerPorts() {
        return config.getAdminServerPorts();
    }
}
