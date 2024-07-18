package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

import com.google.re2j.Pattern;

final class AutoValue_Matchers_HeaderMatcher extends Matchers.HeaderMatcher {

  private final String name;

  @Nullable
  private final String exactValue;

  @Nullable
  private final Pattern safeRegEx;

  @Nullable
  private final Matchers.HeaderMatcher.Range range;

  @Nullable
  private final Boolean present;

  @Nullable
  private final String prefix;

  @Nullable
  private final String suffix;

  @Nullable
  private final String contains;

  @Nullable
  private final Matchers.StringMatcher stringMatcher;

  private final boolean inverted;

  AutoValue_Matchers_HeaderMatcher(
      String name,
      @Nullable String exactValue,
      @Nullable Pattern safeRegEx,
      @Nullable Matchers.HeaderMatcher.Range range,
      @Nullable Boolean present,
      @Nullable String prefix,
      @Nullable String suffix,
      @Nullable String contains,
      @Nullable Matchers.StringMatcher stringMatcher,
      boolean inverted) {
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    this.exactValue = exactValue;
    this.safeRegEx = safeRegEx;
    this.range = range;
    this.present = present;
    this.prefix = prefix;
    this.suffix = suffix;
    this.contains = contains;
    this.stringMatcher = stringMatcher;
    this.inverted = inverted;
  }

  @Override
  public String name() {
    return name;
  }

  @Nullable
  @Override
  public String exactValue() {
    return exactValue;
  }

  @Nullable
  @Override
  public Pattern safeRegEx() {
    return safeRegEx;
  }

  @Nullable
  @Override
  public Matchers.HeaderMatcher.Range range() {
    return range;
  }

  @Nullable
  @Override
  public Boolean present() {
    return present;
  }

  @Nullable
  @Override
  public String prefix() {
    return prefix;
  }

  @Nullable
  @Override
  public String suffix() {
    return suffix;
  }

  @Nullable
  @Override
  public String contains() {
    return contains;
  }

  @Nullable
  @Override
  public Matchers.StringMatcher stringMatcher() {
    return stringMatcher;
  }

  @Override
  public boolean inverted() {
    return inverted;
  }

  @Override
  public String toString() {
    return "HeaderMatcher{"
        + "name=" + name + ", "
        + "exactValue=" + exactValue + ", "
        + "safeRegEx=" + safeRegEx + ", "
        + "range=" + range + ", "
        + "present=" + present + ", "
        + "prefix=" + prefix + ", "
        + "suffix=" + suffix + ", "
        + "contains=" + contains + ", "
        + "stringMatcher=" + stringMatcher + ", "
        + "inverted=" + inverted
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Matchers.HeaderMatcher) {
      Matchers.HeaderMatcher that = (Matchers.HeaderMatcher) o;
      return this.name.equals(that.name())
          && (this.exactValue == null ? that.exactValue() == null : this.exactValue.equals(that.exactValue()))
          && (this.safeRegEx == null ? that.safeRegEx() == null : this.safeRegEx.equals(that.safeRegEx()))
          && (this.range == null ? that.range() == null : this.range.equals(that.range()))
          && (this.present == null ? that.present() == null : this.present.equals(that.present()))
          && (this.prefix == null ? that.prefix() == null : this.prefix.equals(that.prefix()))
          && (this.suffix == null ? that.suffix() == null : this.suffix.equals(that.suffix()))
          && (this.contains == null ? that.contains() == null : this.contains.equals(that.contains()))
          && (this.stringMatcher == null ? that.stringMatcher() == null : this.stringMatcher.equals(that.stringMatcher()))
          && this.inverted == that.inverted();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= (exactValue == null) ? 0 : exactValue.hashCode();
    h$ *= 1000003;
    h$ ^= (safeRegEx == null) ? 0 : safeRegEx.hashCode();
    h$ *= 1000003;
    h$ ^= (range == null) ? 0 : range.hashCode();
    h$ *= 1000003;
    h$ ^= (present == null) ? 0 : present.hashCode();
    h$ *= 1000003;
    h$ ^= (prefix == null) ? 0 : prefix.hashCode();
    h$ *= 1000003;
    h$ ^= (suffix == null) ? 0 : suffix.hashCode();
    h$ *= 1000003;
    h$ ^= (contains == null) ? 0 : contains.hashCode();
    h$ *= 1000003;
    h$ ^= (stringMatcher == null) ? 0 : stringMatcher.hashCode();
    h$ *= 1000003;
    h$ ^= inverted ? 1231 : 1237;
    return h$;
  }

}
