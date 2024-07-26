package org.apache.dubbo.xds.resource.grpc.resource.matcher;

import java.math.BigInteger;
import java.net.InetAddress;

public final class CidrMatcher {

  private final InetAddress addressPrefix;

  private final int prefixLen;

    /** Returns matching result for this address. */
    public boolean matches(InetAddress address) {
        if (address == null) {
            return false;
        }
        byte[] cidr = addressPrefix().getAddress();
        byte[] addr = address.getAddress();
        if (addr.length != cidr.length) {
            return false;
        }
        BigInteger cidrInt = new BigInteger(cidr);
        BigInteger addrInt = new BigInteger(addr);

        int shiftAmount = 8 * cidr.length - prefixLen();

        cidrInt = cidrInt.shiftRight(shiftAmount);
        addrInt = addrInt.shiftRight(shiftAmount);
        return cidrInt.equals(addrInt);
    }

    /** Constructs a CidrMatcher with this prefix and prefix length.
     * Do not provide string addressPrefix constructor to avoid IO exception handling.
     * */
    public static CidrMatcher create(InetAddress addressPrefix, int prefixLen) {
        return new CidrMatcher(addressPrefix, prefixLen);
    }

    CidrMatcher(
      InetAddress addressPrefix,
      int prefixLen) {
    if (addressPrefix == null) {
      throw new NullPointerException("Null addressPrefix");
    }
    this.addressPrefix = addressPrefix;
    this.prefixLen = prefixLen;
  }

  InetAddress addressPrefix() {
    return addressPrefix;
  }

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
    if (o instanceof CidrMatcher) {
        CidrMatcher that = (CidrMatcher) o;
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
