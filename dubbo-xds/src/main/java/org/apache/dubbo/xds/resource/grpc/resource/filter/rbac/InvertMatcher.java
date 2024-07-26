package org.apache.dubbo.xds.resource.grpc.resource.filter.rbac;

final class InvertMatcher implements Matcher {

    private final Matcher toInvertMatcher;

    public static InvertMatcher create(Matcher matcher) {
        return new InvertMatcher(matcher);
    }

    @Override
    public boolean matches(Object args) {
        return !toInvertMatcher().matches(args);
    }

    InvertMatcher(
            Matcher toInvertMatcher) {
        if (toInvertMatcher == null) {
            throw new NullPointerException("Null toInvertMatcher");
        }
        this.toInvertMatcher = toInvertMatcher;
    }

    public Matcher toInvertMatcher() {
        return toInvertMatcher;
    }

    @Override
    public String toString() {
        return "InvertMatcher{" + "toInvertMatcher=" + toInvertMatcher + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof InvertMatcher) {
            InvertMatcher that = (InvertMatcher) o;
            return this.toInvertMatcher.equals(that.toInvertMatcher());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= toInvertMatcher.hashCode();
        return h$;
    }

}
