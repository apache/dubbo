package org.apache.dubbo.xds.resource_new.filter.rbac;

import org.apache.dubbo.xds.resource_new.matcher.HeaderMatcher;

final class AuthHeaderMatcher implements Matcher {

    private final HeaderMatcher delegate;

    public static AuthHeaderMatcher create(HeaderMatcher delegate) {
        return new AuthHeaderMatcher(delegate);
    }

    @Override
    public boolean matches(Object args) {
        return true;
    }

    AuthHeaderMatcher(
            HeaderMatcher delegate) {
        if (delegate == null) {
            throw new NullPointerException("Null delegate");
        }
        this.delegate = delegate;
    }

    public HeaderMatcher delegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return "AuthHeaderMatcher{" + "delegate=" + delegate + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof AuthHeaderMatcher) {
            AuthHeaderMatcher that = (AuthHeaderMatcher) o;
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
