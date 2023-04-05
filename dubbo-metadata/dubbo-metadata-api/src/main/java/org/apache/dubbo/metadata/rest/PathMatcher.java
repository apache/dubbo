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


import java.util.Objects;

/**
 * for http request  path match
 */
public class PathMatcher {
    private static final String SEPARATOR = "/";
    private String path;
    private String version;// service version
    private String group;// service group
    private Integer port;// service port
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

    private void setPath(String path) {
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

    public static PathMatcher getInvokeCreatePathMatcher(String path, String version, String group, Integer port) {
        return new PathMatcher(path, version, group, port);
    }

    public boolean hasPathVariable() {
        return hasPathVariable;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathMatcher that = (PathMatcher) o;
        return pathEqual(that)
            && Objects.equals(version, that.version)
            && Objects.equals(group, that.group) && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, group, port);
    }

    private boolean pathEqual(PathMatcher pathMatcher) {


        // no place hold
        if (!pathMatcher.hasPathVariable) {
            return this.path.equals(pathMatcher.path);
        }

        String[] pathSplits = pathMatcher.pathSplits;
        String[] thisPathSplits = this.pathSplits;


        if (thisPathSplits.length != pathSplits.length) {
            return false;
        }

        for (int i = 0; i < pathSplits.length; i++) {
            boolean equals = thisPathSplits[i].equals(pathSplits[i]);
            if (equals) {
                continue;
            } else {
                if (placeHoldCompare(pathSplits[i], thisPathSplits[i])) {
                    continue;
                } else {
                    return false;
                }
            }
        }

        return true;

    }

    private boolean placeHoldCompare(String pathSplit, String pathToCompare) {
        boolean startAndEndEqual = isPlaceHold(pathSplit) || isPlaceHold(pathToCompare);

        // start {  end }
        if (!startAndEndEqual) {
            return false;
        }

        // exclude  {}
        boolean lengthCondition = pathSplit.length() >= 3 || pathToCompare.length() >= 3;

        if (!lengthCondition) {
            return false;
        }

        return true;
    }

    private boolean isPlaceHold(String pathSplit) {
        return pathSplit.startsWith("{") && pathSplit.endsWith("}");
    }


    private String contextPathFormat(String contextPath) {


        if (contextPath == null || contextPath.equals(SEPARATOR) || contextPath.length() == 0) {
            return "";
        }


        return pathFormat(contextPath);
    }

    private String pathFormat(String path) {
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
            ", hasPathVariable=" + hasPathVariable +
            ", contextPath='" + contextPath + '\'' +
            '}';
    }
}
