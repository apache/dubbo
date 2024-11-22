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
package org.apache.dubbo.rpc.protocol.tri.rest.support.swagger;

import org.apache.dubbo.common.utils.StringUtils;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Helper {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([\\w.-]+)}}");

    private Helper() {}

    public static String render(String text, Function<String, String> fn) {
        if (text == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer(text.length());
        while (matcher.find()) {
            String value = fn.apply(matcher.group(1));
            matcher.appendReplacement(result, value == null ? StringUtils.EMPTY_STRING : value);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
