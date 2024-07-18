package org.apache.dubbo.xds.resource.grpc;

import java.net.InetAddress;

final class AutoValue_Matchers_CidrMatcher extends Matchers.CidrMatcher {

  private final InetAddress addressPrefix;

  private final int prefixLen;

  AutoValue_Matchers_CidrMatcher(
      InetAddress addressPrefix,
      int prefixLen) {
    if (addressPrefix == null) {
      throw new NullPointerException("Null addressPrefix");
    }
    this.addressPrefix = addressPrefix;
    this.prefixLen = prefixLen;
  }

  @Override
  InetAddress addressPrefix() {
    return addressPrefix;
  }

  @Override
  int prefixLen() {
    return prefixLen;
  }

  @Override
  public String toString() {
    return "CidrMatcher{"
        + "addressPrefix=" + addressPrefix + ", "
        + "prefixLen=" + prefixLen
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Matchers.CidrMatcher) {
      Matchers.CidrMatcher that = (Matchers.CidrMatcher) o;
      return this.addressPrefix.equals(that.addressPrefix())
          && this.prefixLen == that.prefixLen();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= addressPrefix.hashCode();
    h$ *= 1000003;
    h$ ^= prefixLen;
    return h$;
  }

}
