package org.apache.dubbo.xds.resource_new.common;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CidrRange {

    private final InetAddress addressPrefix;

    private final int prefixLen;

    CidrRange(
            InetAddress addressPrefix, int prefixLen) {
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
        return "CidrRange{" + "addressPrefix=" + addressPrefix + ", " + "prefixLen=" + prefixLen + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof CidrRange) {
            CidrRange that = (CidrRange) o;
            return this.addressPrefix.equals(that.addressPrefix()) && this.prefixLen == that.prefixLen();
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

    public static CidrRange create(String addressPrefix, int prefixLen) throws UnknownHostException {
        return new CidrRange(InetAddress.getByName(addressPrefix), prefixLen);
    }

}
