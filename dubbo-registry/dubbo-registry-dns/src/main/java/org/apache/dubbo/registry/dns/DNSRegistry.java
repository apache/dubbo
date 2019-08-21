package org.apache.dubbo.registry.dns;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DNSRegistry extends FailbackRegistry {
    private static final Logger logger = LoggerFactory.getLogger(DNSRegistry.class);
    private static final int TIME_OUT = 5000;
    private Map<String, Integer> portMap = new HashMap<>();

    public DNSRegistry(URL url) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }
        try {
            InetAddress[] inetAddresses = InetAddress.getAllByName(url.getHost());
            for (InetAddress inetAddress : inetAddresses) {
                this.portMap.put(inetAddress.getHostAddress(), url.getPort());
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doRegister(URL url) {
        //Register by OP solution
    }

    @Override
    public void doUnregister(URL url) {

    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        List<URL> providers = getProviders(url);
        notify(url, listener, providers);
    }

    private List<URL> getProviders(URL consumer) {
        List<URL> providers = new ArrayList<>();
        //消费者所使用的service与生产者提供的service对应
        String service = consumer.getPath();
        String protocol = consumer.getParameter("protocol");
        for (String address : portMap.keySet()) {
            providers.add(new URL(protocol, address, portMap.get(address),service));
        }
        return providers;
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {

    }

    @Override
    public boolean isAvailable() {
        boolean reachable = false;
        try {
            //available if one is reachable
            for (String address : portMap.keySet()) {
                if (InetAddress.getByName(address).isReachable(TIME_OUT)){
                    reachable = true;
                    return reachable;
                }
            }
        } catch (IOException e) {
            logger.warn(e);
        }
        return reachable;
    }
}
