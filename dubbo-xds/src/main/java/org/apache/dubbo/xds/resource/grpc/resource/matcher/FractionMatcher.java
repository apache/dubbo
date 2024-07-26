package org.apache.dubbo.xds.resource.grpc.resource.matcher;

public final class FractionMatcher {

    private final int numerator;

    private final int denominator;

    public static FractionMatcher create(int numerator, int denominator) {
        return new FractionMatcher(numerator, denominator);
    }

    FractionMatcher(
            int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public int numerator() {
        return numerator;
    }

    public int denominator() {
        return denominator;
    }

    @Override
    public String toString() {
        return "FractionMatcher{" + "numerator=" + numerator + ", " + "denominator=" + denominator + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof FractionMatcher) {
            FractionMatcher that = (FractionMatcher) o;
            return this.numerator == that.numerator() && this.denominator == that.denominator();
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
