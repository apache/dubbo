package org.apache.dubbo.remoting.transport.smartsocket;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.RemotingException;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/25
 */
public class SmartSocketMessageProcessor implements MessageProcessor<Object> {
    private final URL url;

    private final ChannelHandler handler;
    private final Codec2 codec;
    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    public SmartSocketMessageProcessor(URL url, ChannelHandler handler, Codec2 codec) {
        this.url = url;
        this.handler = handler;
        this.codec = codec;
    }

    @Override
    public void process(AioSession session, Object msg) {
        Channel channel = session.getAttachment();
        try {
            handler.received(channel, msg);
        } catch (RemotingException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Channel> getChannels() {
        return channels;
    }

    @Override
    public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
//        if(throwable!=null){
//            throwable.printStackTrace();
//        }
        switch (stateMachineEnum) {
            case NEW_SESSION: {
                SmartSocketChannel channel = new SmartSocketChannel(session, url, handler, codec);
                channels.put(channel.getChannelKey(), channel);
                session.setAttachment(channel);
                try {
                    handler.connected(channel);
                } catch (Exception e) {
//                    e.printStackTrace();
                }

                break;
            }
            case SESSION_CLOSED: {
                SmartSocketChannel channel = session.getAttachment();
                session.setAttachment(null);
                channels.remove(channel.getChannelKey());
                try {
                    handler.disconnected(channel);
                } catch (Exception e) {
//                    e.printStackTrace();
                }

                break;
            }
        }
    }
}
