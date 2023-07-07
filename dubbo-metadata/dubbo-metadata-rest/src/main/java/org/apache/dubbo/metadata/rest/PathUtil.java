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


import org.apache.dubbo.metadata.MetadataConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * is used to parse url pathVariable
 * <p>
 * String[] splits= url.split("/")
 * List<String> strings = Arrays.asList(split);
 * strings.set(UrlSplitIndex, (String) args.get(argIndex));
 */
public class PathUtil {
    private static final String SEPARATOR = MetadataConstants.PATH_SEPARATOR;

    /**
     * generate real path from  rawPath according to argInfo and method args
     *
     * @param rawPath
     * @param argInfos
     * @param args
     * @return
     */
    public static String resolvePathVariable(String rawPath, List<ArgInfo> argInfos, List<Object> args) {

        String[] split = rawPath.split(SEPARATOR);

        List<String> strings = Arrays.asList(split);

        List<ArgInfo> pathArgInfos = new ArrayList<>();

        for (ArgInfo argInfo : argInfos) {
            if (ParamType.PATH.supportAnno(argInfo.getParamAnnotationType())) {
                pathArgInfos.add(argInfo);
            }
        }


        for (ArgInfo pathArgInfo : pathArgInfos) {
            strings.set(pathArgInfo.getUrlSplitIndex(), String.valueOf(args.get(pathArgInfo.getIndex())));
        }


        String pat = SEPARATOR;

        for (String string : strings) {

            if (string.length() == 0) {
                continue;
            }

            pat = pat + string + SEPARATOR;
        }

        if (pat.endsWith(SEPARATOR)) {
            pat = pat.substring(0, pat.lastIndexOf(SEPARATOR));
        }

        return pat;

    }


    /**
     * parse pathVariable index from url by annotation info
     *
     * @param rawPath
     * @param argInfos
     */
    public static void setArgInfoSplitIndex(String rawPath, List<ArgInfo> argInfos) {
        String[] split = rawPath.split(SEPARATOR);

        List<PathPair> pathPairs = new ArrayList<>();

        for (ArgInfo argInfo : argInfos) {
            if (ParamType.PATH.supportAnno(argInfo.getParamAnnotationType())) {
                pathPairs.add(new PathPair(argInfo));
            }
        }

        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            for (PathPair pathPair : pathPairs) {
                boolean match = pathPair.match(s);
                if (match) {
                    pathPair.setArgInfoSplitIndex(i);
                }
            }
        }

    }

    public static class PathPair {

        String value;

        ArgInfo argInfo;


        public PathPair(ArgInfo argInfo) {
            this.argInfo = argInfo;
            this.value = argInfo.getAnnotationNameAttribute();
        }

        public String getPatten() {
            return "{" + value + "}";
        }

        public String getLeftPatten() {
            return "{" + value;
        }

        public String getRightPatten() {
            return "}";
        }

        public boolean match(String value) {
            return getPatten().equals(value)// for : {id}
                || (value.startsWith(getLeftPatten()) && value.endsWith(getRightPatten()));// for : {id: \d+}
        }


        public String getValue() {
            return value;
        }

        public void setArgInfo(ArgInfo argInfo) {
            this.argInfo = argInfo;
        }

        public void setArgInfoSplitIndex(int urlSplitIndex) {
            this.argInfo.setUrlSplitIndex(urlSplitIndex);
        }
    }
}


