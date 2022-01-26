package org.apache.dubbo.remoting.transport.smartsocket;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Client;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.Transporter;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/25
 */
public class SmartSocketTransporter implements Transporter {
    @Override
    public RemotingServer bind(URL url, ChannelHandler handler) throws RemotingException {
        return new SmartSocketServer(url, handler);
    }

    @Override
    public Client connect(URL url, ChannelHandler handler) throws RemotingException {
        return new SmartSocketClient(url, handler);
    }
}
