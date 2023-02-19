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
package org.apache.dubbo.metadata.rest;


import java.util.Arrays;
import java.util.Objects;

public class PathMatcher {
    private static final String SEPARATOR = "/";
    private String path;
    private String version;
    private String group;
    private Integer port;
    private String[] pathSplits;
    private boolean hasPathVariable;
    private String contextPath;


    public PathMatcher(String path) {
        this(path, null, null, null);
    }

    public PathMatcher(String path, String version, String group, Integer port) {
        this.path = path;
        dealPathVariable(path);
        this.version = version;
        this.group = group;
        this.port = (port == null || port == -1 || port == 0) ? null : port;
    }

    private void dealPathVariable(String path) {
        this.pathSplits = path.split(SEPARATOR);

        for (String pathSplit : pathSplits) {

            if (isPlaceHold(pathSplit)) {
                hasPathVariable = true;
                break;
            }
        }
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setContextPath(String contextPath) {


        contextPath = contextPathFormat(contextPath);


        this.contextPath = contextPath;

        setPath(contextPath + path);

        dealPathVariable(path);

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathMatcher that = (PathMatcher) o;
        return pathEqual(that.path) && Objects.equals(version, that.version)
            && Objects.equals(group, that.group) && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, group, port);
    }

    private boolean pathEqual(String path) {

        // no place hold
        if (!hasPathVariable) {
            return this.path.equals(path);
        }

        String[] split = path.split(SEPARATOR);


        if (split.length != pathSplits.length) {
            return false;
        }

        for (int i = 0; i < pathSplits.length; i++) {
            boolean equals = split[i].equals(pathSplits[i]);
            if (equals) {
                continue;
            } else {
                if (placeHoldCompare(pathSplits[i])) {
                    continue;
                } else {
                    return false;
                }
            }
        }

        return true;

    }

    private boolean placeHoldCompare(String pathSplit) {
        boolean startAndEndEqual = isPlaceHold(pathSplit);

        // start {  end }
        if (!startAndEndEqual) {
            return false;
        }

        // exclude  {}
        boolean lengthCondition = pathSplit.length() >= 3;

        if (!lengthCondition) {
            return false;
        }

        return true;
    }

    private boolean isPlaceHold(String pathSplit) {
        return pathSplit.startsWith("{") && pathSplit.endsWith("}");
    }


    private String contextPathFormat(String path) {


        if (path == null || path.equals(SEPARATOR) || path.length() == 0) {
            return "";
        }

        if (path.startsWith(SEPARATOR)) {
            return path;
        } else {
            return SEPARATOR + path;
        }

    }


    @Override
    public String toString() {
        return "PathMatcher{" +
            "path='" + path + '\'' +
            ", version='" + version + '\'' +
            ", group='" + group + '\'' +
            ", port=" + port +
            ", pathSplits=" + Arrays.toString(pathSplits) +
            ", hasPathVariable=" + hasPathVariable +
            ", contextPath='" + contextPath + '\'' +
            '}';
    }
}
