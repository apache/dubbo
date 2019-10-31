package org.apache.dubbo.remoting.utils;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;

public class LogUtils {
    private static Logger logger = LoggerFactory.getLogger(LogUtils.class);

    /**
     * only log body in debugger mode for size & security consideration.
     *
     * @param message
     * @return
     */
    public static Object getRequestWithoutData(Object message) {
        if (logger.isDebugEnabled()) {
            return message;
        }
        if (message instanceof Request) {
            Request request = (Request) message;
            request.setData(null);
            return request;
        } else if (message instanceof Response) {
            Response response = (Response) message;
            response.setResult(null);
            return response;
        }
        return message;
    }
}
