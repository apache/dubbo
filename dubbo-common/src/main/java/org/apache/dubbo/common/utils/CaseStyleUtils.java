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
package org.apache.dubbo.common.utils;

/**
 * The utilities class for Case Styles, for examples: Camel, Pascal, Snake, and Kebab Case
 *
 * @since 2.7.7
 */
public interface CaseStyleUtils {

    /**
     * Resolve the property name from the raw one.
     *
     * @param kebabCaseValue <code>kebabCaseValue</code>
     * @return If the pattern of <code>kebabCaseValue</code> is like "user-name", the resolved result will be "userName"
     * , or return <code>kebabCaseValue</code>
     */
    static String kebabToCamel(String kebabCaseValue) {

        if (StringUtils.isBlank(kebabCaseValue)) {
            return kebabCaseValue;
        }

        int index = kebabCaseValue.indexOf("-");

        if (index == -1) { // the character "-" can't be found
            return kebabCaseValue;
        }

        if (index == kebabCaseValue.length() - 1) { // the character "-" ends with the whole string
            return kebabCaseValue;
        }

        StringBuilder kebabValueBuilder = new StringBuilder(kebabCaseValue);

        while (index > 0) {
            // delete the character "-"
            kebabValueBuilder.deleteCharAt(index);
            // the index of next character has been update to be index
            char nextChar = kebabValueBuilder.charAt(index);
            // the next character to be upper case if possible
            nextChar = Character.toUpperCase(nextChar);
            // re-set the character
            kebabValueBuilder.setCharAt(index, nextChar);
            index = kebabValueBuilder.indexOf("-", index);
        }

        return kebabValueBuilder.toString();
    }

}
