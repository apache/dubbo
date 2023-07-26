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
package org.apache.dubbo.rpc.cluster.router.xds.rule;

public class HeaderMatcher {


    public String name;

    public String exactValue;

    private String regex;

    public LongRangeMatch range;

    public Boolean present;

    public String prefix;

    public String suffix;

    public boolean inverted;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExactValue() {
        return exactValue;
    }

    public void setExactValue(String exactValue) {
        this.exactValue = exactValue;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public LongRangeMatch getRange() {
        return range;
    }

    public void setRange(LongRangeMatch range) {
        this.range = range;
    }

    public Boolean getPresent() {
        return present;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    public boolean match(String input) {
        if (getPresent() != null) {
            return (input == null) == getPresent().equals(isInverted());
        }
        if (input == null) {
            return false;
        }
        if (getExactValue() != null) {
            return getExactValue().equals(input) != isInverted();
        } else if (getRegex() != null) {
            return input.matches(getRegex()) != isInverted();
        } else if (getRange() != null) {
            return getRange().isMatch(input) != isInverted();
        } else if (getPrefix() != null) {
            return input.startsWith(getPrefix()) != isInverted();
        } else if (getSuffix() != null) {
            return input.endsWith(getSuffix()) != isInverted();
        }
        return false;
    }

}
