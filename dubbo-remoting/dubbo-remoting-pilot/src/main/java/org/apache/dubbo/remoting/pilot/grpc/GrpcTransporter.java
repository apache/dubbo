package org.apache.dubbo.remoting.pilot.grpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.pilot.PilotClient;
import org.apache.dubbo.remoting.pilot.PilotTransporter;

/**
 * grpc support for pilot transporter
 * @author hzj
 * @date 2019/03/20
 */
public class GrpcTransporter implements PilotTransporter {
    @Override
    public PilotClient connect(URL url) {
        return new GrpcClient(url);
    }
}
