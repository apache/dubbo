package org.apache.dubbo.remoting.pilot;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.Constants;

/**
 * provider client to pilot registry
 * @author hzj
 * @date 2019/03/20
 */
@SPI("grpc")
public interface PilotTransporter {

    @Adaptive({Constants.CLIENT_KEY, Constants.TRANSPORTER_KEY})
    PilotClient connect(URL url);
}
