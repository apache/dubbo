package org.apache.dubbo.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;

/**
 * @author cvictory ON 2019-02-28
 */
public class CIDRUtilsTest {

    @Test
    public void testIpv4() throws UnknownHostException {
        CIDRUtils cidrUtils = new CIDRUtils("192.168.1.0/26");
        Assertions.assertTrue(cidrUtils.isInRange("192.168.1.63"));
        Assertions.assertFalse(cidrUtils.isInRange("192.168.1.65"));

        cidrUtils = new CIDRUtils("192.168.1.192/26");
        Assertions.assertTrue(cidrUtils.isInRange("192.168.1.199"));
        Assertions.assertFalse(cidrUtils.isInRange("192.168.1.190"));
    }

    @Test
    public void testIpv6() throws UnknownHostException {
        CIDRUtils cidrUtils = new CIDRUtils("234e:0:4567::3d/64");
        Assertions.assertTrue(cidrUtils.isInRange("234e:0:4567::3e"));
        Assertions.assertTrue(cidrUtils.isInRange("234e:0:4567::ffff:3e"));
        Assertions.assertFalse(cidrUtils.isInRange("234e:1:4567::3d"));
        Assertions.assertFalse(cidrUtils.isInRange("234e:0:4567:1::3d"));

        cidrUtils = new CIDRUtils("3FFE:FFFF:0:CC00::/54");
        Assertions.assertTrue(cidrUtils.isInRange("3FFE:FFFF:0:CC00::dd"));
        Assertions.assertTrue(cidrUtils.isInRange("3FFE:FFFF:0:CC00:0000:eeee:0909:dd"));
        Assertions.assertTrue(cidrUtils.isInRange("3FFE:FFFF:0:CC0F:0000:eeee:0909:dd"));

        Assertions.assertFalse(cidrUtils.isInRange("3EFE:FFFE:0:C107::dd"));
        Assertions.assertFalse(cidrUtils.isInRange("1FFE:FFFE:0:CC00::dd"));
    }
}
