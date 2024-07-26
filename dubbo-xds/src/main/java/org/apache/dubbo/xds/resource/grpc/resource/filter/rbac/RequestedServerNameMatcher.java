package org.apache.dubbo.xds.resource.grpc.resource.filter.rbac;

import org.apache.dubbo.xds.resource.grpc.resource.matcher.StringMatcher;

final class RequestedServerNameMatcher implements Matcher {

    private final StringMatcher delegate;

    public static RequestedServerNameMatcher create(StringMatcher delegate) {
        return new RequestedServerNameMatcher(delegate);
    }

    @Override
    public boolean matches(Object args) {
        return true;
    }

    RequestedServerNameMatcher(
            StringMatcher delegate) {
        if (delegate == null) {
            throw new NullPointerException("Null delegate");
        }
        this.delegate = delegate;
    }

    public StringMatcher delegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return "RequestedServerNameMatcher{" + "delegate=" + delegate + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof RequestedServerNameMatcher) {
            RequestedServerNameMatcher that = (RequestedServerNameMatcher) o;
            return this.delegate.equals(that.delegate());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= delegate.hashCode();
        return h$;
    }

}
