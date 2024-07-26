package org.apache.dubbo.xds.resource.grpc.resource.filter.rbac;

import org.apache.dubbo.xds.resource.grpc.resource.matcher.StringMatcher;

final class PathMatcher implements Matcher {

    private final StringMatcher delegate;

    public static PathMatcher create(StringMatcher delegate) {
        return new PathMatcher(delegate);
    }

    @Override
    public boolean matches(Object args) {
        return true;
    }

    PathMatcher(
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
        return "PathMatcher{" + "delegate=" + delegate + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof PathMatcher) {
            PathMatcher that = (PathMatcher) o;
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
