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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match;


public class StringMatch {
    private String exact;
    private String prefix;
    private String regex;
    private String noempty;
    private String empty;


    public String getExact() {
        return exact;
    }

    public void setExact(String exact) {
        this.exact = exact;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getNoempty() {
        return noempty;
    }

    public void setNoempty(String noempty) {
        this.noempty = noempty;
    }

    public String getEmpty() {
        return empty;
    }

    public void setEmpty(String empty) {
        this.empty = empty;
    }

    public boolean isMatch(String input) {
        if (getExact() != null && input != null) {
            return input.equals(getExact());
        } else if (getPrefix() != null && input != null) {
            return input.startsWith(getPrefix());
        } else if (getRegex() != null && input != null) {
            return input.matches(getRegex());
        } else if (getEmpty() != null) {
            return input == null || "".equals(input);
        } else if (getNoempty() != null) {
            return input != null && input.length() > 0;
        } else {
            return false;
        }
    }


    @Override
    public String toString() {
        return "StringMatch{" +
                "exact='" + exact + '\'' +
                ", prefix='" + prefix + '\'' +
                ", regex='" + regex + '\'' +
                ", noempty='" + noempty + '\'' +
                ", empty='" + empty + '\'' +
                '}';
    }
}
