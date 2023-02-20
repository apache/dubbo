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

public class PathMatcher {
    private static final String SEPARATOR = "/";
    private String path;
    private String version;
    private String group;
    private Integer port;
    private String[] pathSplits;
    private boolean hasPathVariable;


    public PathMatcher(String path) {
        this(path, null, null, 0);
    }

    public PathMatcher(String path, String version, String group, Integer port) {
        this.path = path;
        this.pathSplits = path.split(SEPARATOR);

        for (String pathSplit : pathSplits) {

            if (isPlaceHold(pathSplit)) {
                hasPathVariable = true;
                break;
            }
        }
        this.version = version;
        this.group = group;
        this.port = port;
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


    @Override
    public String toString() {
        return "PathMather{" +
            "path='" + path + '\'' +
            ", version='" + version + '\'' +
            ", group='" + group + '\'' +
            ", port='" + port + '\'' +
            '}';
    }
}
