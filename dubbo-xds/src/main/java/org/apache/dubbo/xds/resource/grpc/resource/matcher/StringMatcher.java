package org.apache.dubbo.xds.resource.grpc.resource.matcher;

import org.apache.dubbo.common.lang.Nullable;

import com.google.re2j.Pattern;

import org.apache.dubbo.common.utils.Assert;

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

    /** The input string should exactly matches the specified string. */
    public static StringMatcher forExact(String exact, boolean ignoreCase) {
        Assert.notNull(exact, "exact is null");
        return StringMatcher.create(exact, null, null, null, null,
                ignoreCase);
    }

    /** The input string should have the prefix. */
    public static StringMatcher forPrefix(String prefix, boolean ignoreCase) {
        Assert.notNull(prefix, "prefix is null");
        return StringMatcher.create(null, prefix, null, null, null,
                ignoreCase);
    }

    /** The input string should have the suffix. */
    public static StringMatcher forSuffix(String suffix, boolean ignoreCase) {
        Assert.notNull(suffix, "suffix is null");
        return StringMatcher.create(null, null, suffix, null, null,
                ignoreCase);
    }

    /** The input string should match this pattern. */
    public static StringMatcher forSafeRegEx(Pattern regEx) {
        Assert.notNull(regEx, "regEx is null");
        return StringMatcher.create(null, null, null, regEx, null,
                false/* doesn't matter */);
    }

    /** The input string should contain this substring. */
    public static StringMatcher forContains(String contains) {
        Assert.notNull(contains, "contains is null");
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
        return new StringMatcher(exact, prefix, suffix, regEx, contains,
                ignoreCase);
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
  String exact() {
    return exact;
  }

  @Nullable
  String prefix() {
    return prefix;
  }

  @Nullable
  String suffix() {
    return suffix;
  }

  @Nullable
  Pattern regEx() {
    return regEx;
  }

  @Nullable
  String contains() {
    return contains;
  }

  boolean ignoreCase() {
    return ignoreCase;
  }

  @Override
  public String toString() {
    return "StringMatcher{"
        + "exact=" + exact + ", "
        + "prefix=" + prefix + ", "
        + "suffix=" + suffix + ", "
        + "regEx=" + regEx + ", "
        + "contains=" + contains + ", "
        + "ignoreCase=" + ignoreCase
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof StringMatcher) {
      StringMatcher that = (StringMatcher) o;
      return (this.exact == null ? that.exact() == null : this.exact.equals(that.exact()))
          && (this.prefix == null ? that.prefix() == null : this.prefix.equals(that.prefix()))
          && (this.suffix == null ? that.suffix() == null : this.suffix.equals(that.suffix()))
          && (this.regEx == null ? that.regEx() == null : this.regEx.equals(that.regEx()))
          && (this.contains == null ? that.contains() == null : this.contains.equals(that.contains()))
          && this.ignoreCase == that.ignoreCase();
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
