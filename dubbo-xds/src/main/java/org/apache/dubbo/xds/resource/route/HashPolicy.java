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

    @Nullable
    private final String cookieName;

    @Nullable
    private final String cookiePath;

    @Nullable
    private final Long cookieTtl;

    @Nullable
    private final String queryParameterName;

    @Nullable
    private final Boolean sourceIp;

    @Nullable
    private final String filterStateName;

    public static HashPolicy forHeader(
            boolean isTerminal, String headerName, @Nullable Pattern regEx, @Nullable String regExSubstitution) {
        Assert.notNull(headerName, "headerName must not be null");
        return HashPolicy.create(HashPolicyType.HEADER, isTerminal, headerName, regEx, regExSubstitution, 
            null, null, null, null, null, null);
    }

    public static HashPolicy forChannelId(boolean isTerminal) {
        return HashPolicy.create(HashPolicyType.CHANNEL_ID, isTerminal, null, null, null, 
            null, null, null, null, null, null);
    }

    public static HashPolicy forCookie(
            boolean isTerminal, String cookieName, @Nullable String cookiePath, @Nullable Long cookieTtl) {
        Assert.notNull(cookieName, "cookieName must not be null");
        return HashPolicy.create(HashPolicyType.COOKIE, isTerminal, null, null, null, 
            cookieName, cookiePath, cookieTtl, null, null, null);
    }

    public static HashPolicy forQueryParameter(boolean isTerminal, String queryParameterName) {
        Assert.notNull(queryParameterName, "queryParameterName must not be null");
        return HashPolicy.create(HashPolicyType.QUERY_PARAMETER, isTerminal, null, null, null, 
            null, null, null, queryParameterName, null, null);
    }

    public static HashPolicy forConnectionProperties(boolean isTerminal, Boolean sourceIp) {
        Assert.notNull(sourceIp, "sourceIp must not be null");
        return HashPolicy.create(HashPolicyType.CONNECTION_PROPERTIES, isTerminal, null, null, null, 
            null, null, null, null, sourceIp, null);
    }

    public static HashPolicy forFilterState(boolean isTerminal, String filterStateName) {
        Assert.notNull(filterStateName, "filterStateName must not be null");
        return HashPolicy.create(HashPolicyType.FILTER_STATE, isTerminal, null, null, null, 
            null, null, null, null, null, filterStateName);
    }

    public static HashPolicy create(
            HashPolicyType type,
            boolean isTerminal,
            @Nullable String headerName,
            @Nullable Pattern regEx,
            @Nullable String regExSubstitution,
            @Nullable String cookieName,
            @Nullable String cookiePath,
            @Nullable Long cookieTtl,
            @Nullable String queryParameterName,
            @Nullable Boolean sourceIp,
            @Nullable String filterStateName) {
        return new HashPolicy(type, isTerminal, headerName, regEx, regExSubstitution,
            cookieName, cookiePath, cookieTtl, queryParameterName, sourceIp, filterStateName);
    }

    HashPolicy(
            HashPolicyType type,
            boolean isTerminal,
            @Nullable String headerName,
            @Nullable Pattern regEx,
            @Nullable String regExSubstitution,
            @Nullable String cookieName,
            @Nullable String cookiePath,
            @Nullable Long cookieTtl,
            @Nullable String queryParameterName,
            @Nullable Boolean sourceIp,
            @Nullable String filterStateName) {
        if (type == null) {
            throw new NullPointerException("Null type");
        }
        this.type = type;
        this.isTerminal = isTerminal;
        this.headerName = headerName;
        this.regEx = regEx;
        this.regExSubstitution = regExSubstitution;
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.cookieTtl = cookieTtl;
        this.queryParameterName = queryParameterName;
        this.sourceIp = sourceIp;
        this.filterStateName = filterStateName;
    }

    public HashPolicyType getType() {
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

    @Nullable
    public String getCookieName() {
        return cookieName;
    }

    @Nullable
    public String getCookiePath() {
        return cookiePath;
    }

    @Nullable
    public Long getCookieTtl() {
        return cookieTtl;
    }

    @Nullable
    public String getQueryParameterName() {
        return queryParameterName;
    }

    @Nullable
    public Boolean getSourceIp() {
        return sourceIp;
    }

    @Nullable
    public String getFilterStateName() {
        return filterStateName;
    }

    @Override
    public String toString() {
        return "HashPolicy{" + "type=" + type + ", " + "isTerminal=" + isTerminal + ", " + "headerName=" + headerName
                + ", " + "regEx=" + regEx + ", " + "regExSubstitution=" + regExSubstitution 
                + ", " + "cookieName=" + cookieName + ", " + "cookiePath=" + cookiePath 
                + ", " + "cookieTtl=" + cookieTtl + ", " + "queryParameterName=" + queryParameterName
                + ", " + "sourceIp=" + sourceIp + ", " + "filterStateName=" + filterStateName + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof HashPolicy) {
            HashPolicy that = (HashPolicy) o;
            return this.type.equals(that.getType())
                    && this.isTerminal == that.isTerminal()
                    && (this.headerName == null
                            ? that.getHeaderName() == null
                            : this.headerName.equals(that.getHeaderName()))
                    && (this.regEx == null ? that.getRegEx() == null : this.regEx.equals(that.getRegEx()))
                    && (this.regExSubstitution == null
                            ? that.getRegExSubstitution() == null
                            : this.regExSubstitution.equals(that.getRegExSubstitution()))
                    && (this.cookieName == null
                            ? that.getCookieName() == null
                            : this.cookieName.equals(that.getCookieName()))
                    && (this.cookiePath == null
                            ? that.getCookiePath() == null
                            : this.cookiePath.equals(that.getCookiePath()))
                    && (this.cookieTtl == null
                            ? that.getCookieTtl() == null
                            : this.cookieTtl.equals(that.getCookieTtl()))
                    && (this.queryParameterName == null
                            ? that.getQueryParameterName() == null
                            : this.queryParameterName.equals(that.getQueryParameterName()))
                    && (this.sourceIp == null
                            ? that.getSourceIp() == null
                            : this.sourceIp.equals(that.getSourceIp()))
                    && (this.filterStateName == null
                            ? that.getFilterStateName() == null
                            : this.filterStateName.equals(that.getFilterStateName()));
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
        h$ *= 1000003;
        h$ ^= (cookieName == null) ? 0 : cookieName.hashCode();
        h$ *= 1000003;
        h$ ^= (cookiePath == null) ? 0 : cookiePath.hashCode();
        h$ *= 1000003;
        h$ ^= (cookieTtl == null) ? 0 : cookieTtl.hashCode();
        h$ *= 1000003;
        h$ ^= (queryParameterName == null) ? 0 : queryParameterName.hashCode();
        h$ *= 1000003;
        h$ ^= (sourceIp == null) ? 0 : sourceIp.hashCode();
        h$ *= 1000003;
        h$ ^= (filterStateName == null) ? 0 : filterStateName.hashCode();
        return h$;
    }
}
