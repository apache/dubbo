package com.alibaba.dubbo.remoting.exchange;

/**
 * 2019-06-20
 */
@Deprecated
public interface ResponseCallback {
    /**
     * done.
     *
     * @param response
     */
    void done(Object response);

    /**
     * caught exception.
     *
     * @param exception
     */
    void caught(Throwable exception);

}
