package org.apache.dubbo.xds.resource.grpc.resource.filter.rbac;

import org.apache.dubbo.xds.resource.grpc.resource.matcher.CidrMatcher;

final class SourceIpMatcher implements Matcher {

  private final CidrMatcher delegate;

    public static SourceIpMatcher create(CidrMatcher delegate) {
        return new SourceIpMatcher(delegate);
    }

    @Override
    public boolean matches(Object args) {
        return true;
    }

    SourceIpMatcher(
      CidrMatcher delegate) {
    if (delegate == null) {
      throw new NullPointerException("Null delegate");
    }
    this.delegate = delegate;
  }

  public CidrMatcher delegate() {
    return delegate;
  }

  @Override
  public String toString() {
    return "SourceIpMatcher{"
        + "delegate=" + delegate
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SourceIpMatcher) {
        SourceIpMatcher that = (SourceIpMatcher) o;
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
