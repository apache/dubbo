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
package org.apache.dubbo.xds.resource_new.matcher;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.utils.Assert;

import com.google.re2j.Pattern;

public final class StringMatcher {

    @Nullable
    private final String exact;

    @Nullable
    private final String prefix;

    @Nullable
    private final String suffix;

    @Nullable
    private final Pattern regEx;

    @Nullable
    private final String contains;

    private final boolean ignoreCase;

    /**
     * The input string should exactly matches the specified string.
     */
    public static StringMatcher forExact(String exact, boolean ignoreCase) {
        Assert.notNull(exact, "exact must not be null");
        return StringMatcher.create(exact, null, null, null, null, ignoreCase);
    }

    /**
     * The input string should have the prefix.
     */
    public static StringMatcher forPrefix(String prefix, boolean ignoreCase) {
        Assert.notNull(prefix, "prefix must not be null");
        return StringMatcher.create(null, prefix, null, null, null, ignoreCase);
    }

    /**
     * The input string should have the suffix.
     */
    public static StringMatcher forSuffix(String suffix, boolean ignoreCase) {
        Assert.notNull(suffix, "suffix must not be null");
        return StringMatcher.create(null, null, suffix, null, null, ignoreCase);
    }

    /**
     * The input string should match this pattern.
     */
    public static StringMatcher forSafeRegEx(Pattern regEx) {
        Assert.notNull(regEx, "regEx must not be null");
        return StringMatcher.create(null, null, null, regEx, null, false /* doesn't matter */);
    }

    /**
     * The input string should contain this substring.
     */
    public static StringMatcher forContains(String contains) {
        Assert.notNull(contains, "contains must not be null");
        return StringMatcher.create(null, null, null, null, contains, false /* doesn't matter */);
    }

    /**
     * Returns the matching result for this string.
     */
    public boolean matches(String args) {
        if (args == null) {
            return false;
        }
        if (getExact() != null) {
            return isIgnoreCase()
                    ? getExact().equalsIgnoreCase(args)
                    : getExact().equals(args);
        } else if (getPrefix() != null) {
            return isIgnoreCase()
                    ? args.toLowerCase().startsWith(getPrefix().toLowerCase())
                    : args.startsWith(getPrefix());
        } else if (getSuffix() != null) {
            return isIgnoreCase() ? args.toLowerCase().endsWith(getSuffix().toLowerCase()) : args.endsWith(getSuffix());
        } else if (getContains() != null) {
            return args.contains(getContains());
        }
        return getRegEx().matches(args);
    }

    private static StringMatcher create(
            @Nullable String exact,
            @Nullable String prefix,
            @Nullable String suffix,
            @Nullable Pattern regEx,
            @Nullable String contains,
            boolean ignoreCase) {
        return new StringMatcher(exact, prefix, suffix, regEx, contains, ignoreCase);
    }

    StringMatcher(
            @Nullable String exact,
            @Nullable String prefix,
            @Nullable String suffix,
            @Nullable Pattern regEx,
            @Nullable String contains,
            boolean ignoreCase) {
        this.exact = exact;
        this.prefix = prefix;
        this.suffix = suffix;
        this.regEx = regEx;
        this.contains = contains;
        this.ignoreCase = ignoreCase;
    }

    @Nullable
    public String getExact() {
        return exact;
    }

    @Nullable
    public String getPrefix() {
        return prefix;
    }

    @Nullable
    public String getSuffix() {
        return suffix;
    }

    @Nullable
    public Pattern getRegEx() {
        return regEx;
    }

    @Nullable
    public String getContains() {
        return contains;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    @Override
    public String toString() {
        return "StringMatcher{" + "exact=" + exact + ", " + "prefix=" + prefix + ", " + "suffix=" + suffix + ", "
                + "regEx=" + regEx + ", " + "contains=" + contains + ", " + "ignoreCase=" + ignoreCase + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof StringMatcher) {
            StringMatcher that = (StringMatcher) o;
            return (this.exact == null ? that.getExact() == null : this.exact.equals(that.getExact()))
                    && (this.prefix == null ? that.getPrefix() == null : this.prefix.equals(that.getPrefix()))
                    && (this.suffix == null ? that.getSuffix() == null : this.suffix.equals(that.getSuffix()))
                    && (this.regEx == null ? that.getRegEx() == null : this.regEx.equals(that.getRegEx()))
                    && (this.contains == null ? that.getContains() == null : this.contains.equals(that.getContains()))
                    && this.ignoreCase == that.isIgnoreCase();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (exact == null) ? 0 : exact.hashCode();
        h$ *= 1000003;
        h$ ^= (prefix == null) ? 0 : prefix.hashCode();
        h$ *= 1000003;
        h$ ^= (suffix == null) ? 0 : suffix.hashCode();
        h$ *= 1000003;
        h$ ^= (regEx == null) ? 0 : regEx.hashCode();
        h$ *= 1000003;
        h$ ^= (contains == null) ? 0 : contains.hashCode();
        h$ *= 1000003;
        h$ ^= ignoreCase ? 1231 : 1237;
        return h$;
    }
}
