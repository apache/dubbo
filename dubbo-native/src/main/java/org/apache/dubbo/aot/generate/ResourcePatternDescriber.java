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
package org.apache.dubbo.aot.generate;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A describer that describes resources that should be made available at runtime.
 */
public class ResourcePatternDescriber implements ConditionalDescriber {

    private final String pattern;

    private final String reachableType;

    public ResourcePatternDescriber(String pattern, String reachableType) {
        this.pattern = pattern;
        this.reachableType = reachableType;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public String getReachableType() {
        return reachableType;
    }

    public Pattern toRegex() {
        String prefix = (this.pattern.startsWith("*") ? ".*" : "");
        String suffix = (this.pattern.endsWith("*") ? ".*" : "");
        String regex = Arrays.stream(this.pattern.split("\\*"))
            .filter(s -> !s.isEmpty())
            .map(Pattern::quote)
            .collect(Collectors.joining(".*", prefix, suffix));
        return Pattern.compile(regex);
    }



}
