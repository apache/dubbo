package org.apache.dubbo.remoting.pilot;

/**
 * @author hzj
 * @create 2020/1/11
 */
public interface Constants {

    public static final String  SESSION_TIMEOUT_KEY                = "session";

    public static final int     DEFAULT_SESSION_TIMEOUT            = 30 * 1000;

    public static final String  REGISTRY_RECONNECT_PERIOD_KEY      = "reconnect.period";

    public static final int     DEFAULT_REGISTRY_RECONNECT_PERIOD  = 3 * 1000;
}
