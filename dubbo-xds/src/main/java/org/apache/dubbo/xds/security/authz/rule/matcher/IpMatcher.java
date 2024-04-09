package org.apache.dubbo.xds.security.authz.rule.matcher;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

public class IpMatcher implements Matcher<String>{

    /**
     * Prefix length in CIDR case
     */
    private final int prefixLen;

    /**
     * Ip address to be matched
     */
    private final String ipBinaryString;

    private final RequestAuthProperty authProperty;

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(IpMatcher.class);

    public IpMatcher(int prefixLen, String ipString, RequestAuthProperty property)  {
        this.prefixLen = prefixLen;
        this.ipBinaryString = ip2BinaryString(ipString);
        this.authProperty = property;
    }

    /**
     * @param ip dotted ip string,
     * @return
     */
    public static String ip2BinaryString(String ip) {
        try {
            String[] ips = ip.split("\\.");
            if (4 != ips.length) {
                logger.error("","","","Error ip="+ip);
                return "";
            }
            long[] ipLong = new long[4];
            for (int i = 0; i < 4; ++i) {
                ipLong[i] = Long.parseLong(ips[i]);
                if (ipLong[i] < 0 || ipLong[i] > 255) {
                    logger.error("","","","Error ip="+ip);
                    return "";
                }
            }
            return String.format("%32s", Long.toBinaryString((ipLong[0] << 24)
                    + (ipLong[1] << 16)
                    + (ipLong[2] << 8)
                    + ipLong[3])).replace(" ", "0");
        } catch (Exception e) {
            logger.error("","","","Error ip="+ ip);
        }
        return "";
    }

    public boolean match(String object) {
        if (StringUtils.isEmpty(ipBinaryString)) {
            return false;
        }
        String ipBinary = ip2BinaryString(object);
        if (StringUtils.isEmpty(ipBinary)) {
            return false;
        }
        if (prefixLen <= 0) {
            return ipBinaryString.equals(ipBinary);
        }
        if (ipBinaryString.length() >= prefixLen && ipBinary.length() >= prefixLen) {
            return ipBinaryString.substring(0, prefixLen)
                    .equals(ipBinary.substring(0, prefixLen));
        }
        return false;
    }

    @Override
    public RequestAuthProperty propType() {
        return authProperty;
    }

    public int getPrefixLen() {
        return prefixLen;
    }

    public String getIpBinaryString() {
        return ipBinaryString;
    }
}
