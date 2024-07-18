package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_Matchers_FractionMatcher extends Matchers.FractionMatcher {

  private final int numerator;

  private final int denominator;

  AutoValue_Matchers_FractionMatcher(
      int numerator,
      int denominator) {
    this.numerator = numerator;
    this.denominator = denominator;
  }

  @Override
  public int numerator() {
    return numerator;
  }

  @Override
  public int denominator() {
    return denominator;
  }

  @Override
  public String toString() {
    return "FractionMatcher{"
        + "numerator=" + numerator + ", "
        + "denominator=" + denominator
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Matchers.FractionMatcher) {
      Matchers.FractionMatcher that = (Matchers.FractionMatcher) o;
      return this.numerator == that.numerator()
          && this.denominator == that.denominator();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= numerator;
    h$ *= 1000003;
    h$ ^= denominator;
    return h$;
  }

}
