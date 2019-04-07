package org.apache.dubbo.metadata.store.redis.support;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import redis.clients.jedis.HostAndPort;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * redis url helper
 *
 * @author wuhulala
 * @date 2019/4/7
 */
public class RedisUrlUtils {

    /**
     * parse url
     *
     * @param url url such as redis://127.0.0.1:6379,127.0.0.1:6380
     * @return such as return {HostAndPort("127.0.0.1",6379), HostAndPort("127.0.0.1",6380)}
     */
    public static Set<HostAndPort> parseHostAndPorts(URL url) {
        Set<HostAndPort> hostAndPortsSet = new LinkedHashSet<>(32);
        String hapStr = url.getAddress();
        if (StringUtils.isNotEmpty(hapStr)) {
            String[] nodes = Constants.COMMA_SPLIT_PATTERN.split(hapStr);
            for (String node : nodes) {
                int splitIndex = node.indexOf(":");
                String host = node.substring(0, splitIndex);
                int port = Integer.valueOf(node.substring(splitIndex + 1));
                hostAndPortsSet.add(new HostAndPort(host, port));
            }
        }
        return hostAndPortsSet;
    }
}
