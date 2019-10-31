package org.apache.dubbo.registry.consul;

/**
 * @author cvictory ON 2019-08-02
 */
public class AbstractConsulRegistry {

    static final String SERVICE_TAG = "dubbo";
    static final String URL_META_KEY = "url";
    static final String WATCH_TIMEOUT = "consul-watch-timeout";
    static final String CHECK_PASS_INTERVAL = "consul-check-pass-interval";
    static final String DEREGISTER_AFTER = "consul-deregister-critical-service-after";

    static final int DEFAULT_PORT = 8500;
    // default watch timeout in millisecond
    static final int DEFAULT_WATCH_TIMEOUT = 60 * 1000;
    // default time-to-live in millisecond
    static final long DEFAULT_CHECK_PASS_INTERVAL = 16000L;
    // default deregister critical server after
    static final String DEFAULT_DEREGISTER_TIME = "20s";


}
