package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

import com.google.re2j.Pattern;

final class AutoValue_Matchers_StringMatcher extends Matchers.StringMatcher {

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

  AutoValue_Matchers_StringMatcher(
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
  @Override
  String exact() {
    return exact;
  }

  @Nullable
  @Override
  String prefix() {
    return prefix;
  }

  @Nullable
  @Override
  String suffix() {
    return suffix;
  }

  @Nullable
  @Override
  Pattern regEx() {
    return regEx;
  }

  @Nullable
  @Override
  String contains() {
    return contains;
  }

  @Override
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
    if (o instanceof Matchers.StringMatcher) {
      Matchers.StringMatcher that = (Matchers.StringMatcher) o;
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
