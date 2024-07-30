package org.apache.dubbo.xds.resource_new.common;

public final class FractionalPercent {

    enum DenominatorType {
        HUNDRED,
        TEN_THOUSAND,
        MILLION
    }

    private final int numerator;

    private final DenominatorType denominatorType;

    public static FractionalPercent perHundred(int numerator) {
        return FractionalPercent.create(numerator, FractionalPercent.DenominatorType.HUNDRED);
    }

    public static FractionalPercent perTenThousand(int numerator) {
        return FractionalPercent.create(numerator, FractionalPercent.DenominatorType.TEN_THOUSAND);
    }

    public static FractionalPercent perMillion(int numerator) {
        return FractionalPercent.create(numerator, FractionalPercent.DenominatorType.MILLION);
    }

    public static FractionalPercent create(
            int numerator, FractionalPercent.DenominatorType denominatorType) {
        return new FractionalPercent(numerator, denominatorType);
    }

    public FractionalPercent(
            int numerator, DenominatorType denominatorType) {
        this.numerator = numerator;
        if (denominatorType == null) {
            throw new NullPointerException("Null denominatorType");
        }
        this.denominatorType = denominatorType;
    }

    int numerator() {
        return numerator;
    }

    DenominatorType denominatorType() {
        return denominatorType;
    }

    @Override
    public String toString() {
        return "FractionalPercent{" + "numerator=" + numerator + ", " + "denominatorType=" + denominatorType + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof FractionalPercent) {
            FractionalPercent that = (FractionalPercent) o;
            return this.numerator == that.numerator() && this.denominatorType.equals(that.denominatorType());
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
