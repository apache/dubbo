/*
 * Copyright 2021 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource.grpc;

import com.google.auto.value.AutoValue;
import com.google.re2j.Pattern;

import javax.annotation.Nullable;

import java.math.BigInteger;
import java.net.InetAddress;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides a group of request matchers. A matcher evaluates an input and tells whether certain
 * argument in the input matches a predefined matching pattern.
 */
public final class Matchers {
  // Prevent instantiation.
  private Matchers() {}

  /** Matcher for HTTP request headers. */
  @AutoValue
  public abstract static class HeaderMatcher {
    // Name of the header to be matched.
    public abstract String name();

    // Matches exact header value.
    @Nullable
    public abstract String exactValue();

    // Matches header value with the regular expression pattern.
    @Nullable
    public abstract Pattern safeRegEx();

    // Matches header value an integer value in the range.
    @Nullable
    public abstract Range range();

    // Matches header presence.
    @Nullable
    public abstract Boolean present();

    // Matches header value with the prefix.
    @Nullable
    public abstract String prefix();

    // Matches header value with the suffix.
    @Nullable
    public abstract String suffix();

    // Matches header value with the substring.
    @Nullable
    public abstract String contains();

    // Matches header value with the string matcher.
    @Nullable
    public abstract StringMatcher stringMatcher();

    // Whether the matching semantics is inverted. E.g., present && !inverted -> !present
    public abstract boolean inverted();

    /** The request header value should exactly match the specified value. */
    public static HeaderMatcher forExactValue(String name, String exactValue, boolean inverted) {
      checkNotNull(name, "name");
      checkNotNull(exactValue, "exactValue");
      return HeaderMatcher.create(
        name, exactValue, null, null, null, null, null, null, null, inverted);
    }

    /** The request header value should match the regular expression pattern. */
    public static HeaderMatcher forSafeRegEx(String name, Pattern safeRegEx, boolean inverted) {
      checkNotNull(name, "name");
      checkNotNull(safeRegEx, "safeRegEx");
      return HeaderMatcher.create(
        name, null, safeRegEx, null, null, null, null, null, null, inverted);
    }

    /** The request header value should be within the range. */
    public static HeaderMatcher forRange(String name, Range range, boolean inverted) {
      checkNotNull(name, "name");
      checkNotNull(range, "range");
      return HeaderMatcher.create(name, null, null, range, null, null, null, null, null, inverted);
    }

    /** The request header value should exist. */
    public static HeaderMatcher forPresent(String name, boolean present, boolean inverted) {
      checkNotNull(name, "name");
      return HeaderMatcher.create(
        name, null, null, null, present, null, null, null, null, inverted);
    }

    /** The request header value should have this prefix. */
    public static HeaderMatcher forPrefix(String name, String prefix, boolean inverted) {
      checkNotNull(name, "name");
      checkNotNull(prefix, "prefix");
      return HeaderMatcher.create(name, null, null, null, null, prefix, null, null, null, inverted);
    }

    /** The request header value should have this suffix. */
    public static HeaderMatcher forSuffix(String name, String suffix, boolean inverted) {
      checkNotNull(name, "name");
      checkNotNull(suffix, "suffix");
      return HeaderMatcher.create(name, null, null, null, null, null, suffix, null, null, inverted);
    }

    /** The request header value should have this substring. */
    public static HeaderMatcher forContains(String name, String contains, boolean inverted) {
      checkNotNull(name, "name");
      checkNotNull(contains, "contains");
      return HeaderMatcher.create(
        name, null, null, null, null, null, null, contains, null, inverted);
    }

    /** The request header value should match this stringMatcher. */
    public static HeaderMatcher forString(
        String name, StringMatcher stringMatcher, boolean inverted) {
      checkNotNull(name, "name");
      checkNotNull(stringMatcher, "stringMatcher");
      return HeaderMatcher.create(
        name, null, null, null, null, null, null, null, stringMatcher, inverted);
    }

    private static HeaderMatcher create(String name, @Nullable String exactValue,
        @Nullable Pattern safeRegEx, @Nullable Range range,
        @Nullable Boolean present, @Nullable String prefix,
        @Nullable String suffix, @Nullable String contains,
        @Nullable StringMatcher stringMatcher, boolean inverted) {
      checkNotNull(name, "name");
      return new AutoValue_Matchers_HeaderMatcher(name, exactValue, safeRegEx, range, present,
          prefix, suffix, contains, stringMatcher, inverted);
    }

    /** Returns the matching result. */
    public boolean matches(@Nullable String value) {
      if (value == null) {
        return present() != null && present() == inverted();
      }
      boolean baseMatch;
      if (exactValue() != null) {
        baseMatch = exactValue().equals(value);
      } else if (safeRegEx() != null) {
        baseMatch = safeRegEx().matches(value);
      } else if (range() != null) {
        long numValue;
        try {
          numValue = Long.parseLong(value);
          baseMatch = numValue >= range().start()
              && numValue <= range().end();
        } catch (NumberFormatException ignored) {
          baseMatch = false;
        }
      } else if (prefix() != null) {
        baseMatch = value.startsWith(prefix());
      } else if (present() != null) {
        baseMatch = present();
      } else if (suffix() != null) {
        baseMatch = value.endsWith(suffix());
      } else if (contains() != null) {
        baseMatch = value.contains(contains());
      } else {
        baseMatch = stringMatcher().matches(value);
      }
      return baseMatch != inverted();
    }

    /** Represents an integer range. */
    @AutoValue
    public abstract static class Range {
      public abstract long start();

      public abstract long end();

      public static Range create(long start, long end) {
        return new AutoValue_Matchers_HeaderMatcher_Range(start, end);
      }
    }
  }

  /** Represents a fractional value. */
  @AutoValue
  public abstract static class FractionMatcher {
    public abstract int numerator();

    public abstract int denominator();

    public static FractionMatcher create(int numerator, int denominator) {
      return new AutoValue_Matchers_FractionMatcher(numerator, denominator);
    }
  }

  /** Represents various ways to match a string .*/
  @AutoValue
  public abstract static class StringMatcher {
    @Nullable
    abstract String exact();

    // The input string has this prefix.
    @Nullable
    abstract String prefix();

    // The input string has this suffix.
    @Nullable
    abstract String suffix();

    // The input string matches the regular expression.
    @Nullable
    abstract Pattern regEx();

    // The input string has this substring.
    @Nullable
    abstract String contains();

    // If true, exact/prefix/suffix matching should be case insensitive.
    abstract boolean ignoreCase();

    /** The input string should exactly matches the specified string. */
    public static StringMatcher forExact(String exact, boolean ignoreCase) {
      checkNotNull(exact, "exact");
      return StringMatcher.create(exact, null, null, null, null,
          ignoreCase);
    }

    /** The input string should have the prefix. */
    public static StringMatcher forPrefix(String prefix, boolean ignoreCase) {
      checkNotNull(prefix, "prefix");
      return StringMatcher.create(null, prefix, null, null, null,
          ignoreCase);
    }

    /** The input string should have the suffix. */
    public static StringMatcher forSuffix(String suffix, boolean ignoreCase) {
      checkNotNull(suffix, "suffix");
      return StringMatcher.create(null, null, suffix, null, null,
          ignoreCase);
    }

    /** The input string should match this pattern. */
    public static StringMatcher forSafeRegEx(Pattern regEx) {
      checkNotNull(regEx, "regEx");
      return StringMatcher.create(null, null, null, regEx, null,
          false/* doesn't matter */);
    }

    /** The input string should contain this substring. */
    public static StringMatcher forContains(String contains) {
      checkNotNull(contains, "contains");
      return StringMatcher.create(null, null, null, null, contains,
          false/* doesn't matter */);
    }

    /** Returns the matching result for this string. */
    public boolean matches(String args) {
      if (args == null) {
        return false;
      }
      if (exact() != null) {
        return ignoreCase()
            ? exact().equalsIgnoreCase(args)
            : exact().equals(args);
      } else if (prefix() != null) {
        return ignoreCase()
            ? args.toLowerCase().startsWith(prefix().toLowerCase())
            : args.startsWith(prefix());
      } else if (suffix() != null) {
        return ignoreCase()
            ? args.toLowerCase().endsWith(suffix().toLowerCase())
            : args.endsWith(suffix());
      } else if (contains() != null) {
        return args.contains(contains());
      }
      return regEx().matches(args);
    }

    private static StringMatcher create(@Nullable String exact, @Nullable String prefix,
        @Nullable String suffix, @Nullable Pattern regEx, @Nullable String contains,
        boolean ignoreCase) {
      return new AutoValue_Matchers_StringMatcher(exact, prefix, suffix, regEx, contains,
          ignoreCase);
    }
  }

  /** Matcher to evaluate whether an IPv4 or IPv6 address is within a CIDR range. */
  @AutoValue
  public abstract static class CidrMatcher {

    abstract InetAddress addressPrefix();

    abstract int prefixLen();

    /** Returns matching result for this address. */
    public boolean matches(InetAddress address) {
      if (address == null) {
        return false;
      }
      byte[] cidr = addressPrefix().getAddress();
      byte[] addr = address.getAddress();
      if (addr.length != cidr.length) {
        return false;
      }
      BigInteger cidrInt = new BigInteger(cidr);
      BigInteger addrInt = new BigInteger(addr);

      int shiftAmount = 8 * cidr.length - prefixLen();

      cidrInt = cidrInt.shiftRight(shiftAmount);
      addrInt = addrInt.shiftRight(shiftAmount);
      return cidrInt.equals(addrInt);
    }

    /** Constructs a CidrMatcher with this prefix and prefix length.
     * Do not provide string addressPrefix constructor to avoid IO exception handling.
     * */
    public static CidrMatcher create(InetAddress addressPrefix, int prefixLen) {
      return new AutoValue_Matchers_CidrMatcher(addressPrefix, prefixLen);
    }
  }
}
