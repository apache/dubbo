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
package org.apache.dubbo.xds.resource.route;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.utils.Assert;

import com.google.re2j.Pattern;

public class HashPolicy {

    private final HashPolicyType type;

    private final boolean isTerminal;

    @Nullable
    private final String headerName;

    @Nullable
    private final Pattern regEx;

    @Nullable
    private final String regExSubstitution;

    public static HashPolicy forHeader(
            boolean isTerminal, String headerName, @Nullable Pattern regEx, @Nullable String regExSubstitution) {
        Assert.notNull(headerName, "headerName must not be null");
        return HashPolicy.create(HashPolicyType.HEADER, isTerminal, headerName, regEx, regExSubstitution);
    }

    public static HashPolicy forChannelId(boolean isTerminal) {
        return HashPolicy.create(HashPolicyType.CHANNEL_ID, isTerminal, null, null, null);
    }

    public static HashPolicy create(
            HashPolicyType type,
            boolean isTerminal,
            @Nullable String headerName,
            @Nullable Pattern regEx,
            @Nullable String regExSubstitution) {
        return new HashPolicy(type, isTerminal, headerName, regEx, regExSubstitution);
    }

    HashPolicy(
            HashPolicyType type,
            boolean isTerminal,
            @Nullable String headerName,
            @Nullable Pattern regEx,
            @Nullable String regExSubstitution) {
        if (type == null) {
            throw new NullPointerException("Null type");
        }
        this.type = type;
        this.isTerminal = isTerminal;
        this.headerName = headerName;
        this.regEx = regEx;
        this.regExSubstitution = regExSubstitution;
    }

    HashPolicyType type() {
        return type;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    @Nullable
    public String getHeaderName() {
        return headerName;
    }

    @Nullable
    public Pattern getRegEx() {
        return regEx;
    }

    @Nullable
    public String getRegExSubstitution() {
        return regExSubstitution;
    }

    @Override
    public String toString() {
        return "HashPolicy{" + "type=" + type + ", " + "isTerminal=" + isTerminal + ", " + "headerName=" + headerName
                + ", " + "regEx=" + regEx + ", " + "regExSubstitution=" + regExSubstitution + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof HashPolicy) {
            HashPolicy that = (HashPolicy) o;
            return this.type.equals(that.type())
                    && this.isTerminal == that.isTerminal()
                    && (this.headerName == null
                            ? that.getHeaderName() == null
                            : this.headerName.equals(that.getHeaderName()))
                    && (this.regEx == null ? that.getRegEx() == null : this.regEx.equals(that.getRegEx()))
                    && (this.regExSubstitution == null
                            ? that.getRegExSubstitution() == null
                            : this.regExSubstitution.equals(that.getRegExSubstitution()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= type.hashCode();
        h$ *= 1000003;
        h$ ^= isTerminal ? 1231 : 1237;
        h$ *= 1000003;
        h$ ^= (headerName == null) ? 0 : headerName.hashCode();
        h$ *= 1000003;
        h$ ^= (regEx == null) ? 0 : regEx.hashCode();
        h$ *= 1000003;
        h$ ^= (regExSubstitution == null) ? 0 : regExSubstitution.hashCode();
        return h$;
    }
}
