package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_FaultConfig_FractionalPercent extends FaultConfig.FractionalPercent {

  private final int numerator;

  private final FaultConfig.FractionalPercent.DenominatorType denominatorType;

  AutoValue_FaultConfig_FractionalPercent(
      int numerator,
      FaultConfig.FractionalPercent.DenominatorType denominatorType) {
    this.numerator = numerator;
    if (denominatorType == null) {
      throw new NullPointerException("Null denominatorType");
    }
    this.denominatorType = denominatorType;
  }

  @Override
  int numerator() {
    return numerator;
  }

  @Override
  FaultConfig.FractionalPercent.DenominatorType denominatorType() {
    return denominatorType;
  }

  @Override
  public String toString() {
    return "FractionalPercent{"
        + "numerator=" + numerator + ", "
        + "denominatorType=" + denominatorType
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof FaultConfig.FractionalPercent) {
      FaultConfig.FractionalPercent that = (FaultConfig.FractionalPercent) o;
      return this.numerator == that.numerator()
          && this.denominatorType.equals(that.denominatorType());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= numerator;
    h$ *= 1000003;
    h$ ^= denominatorType.hashCode();
    return h$;
  }

}
