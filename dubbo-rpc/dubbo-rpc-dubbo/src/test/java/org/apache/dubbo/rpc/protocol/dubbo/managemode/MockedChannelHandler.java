package org.apache.dubbo.rpc.protocol.dubbo.managemode;

import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

import java.util.Collections;
import java.util.Set;

public class MockedChannelHandler implements ChannelHandler {
    //    ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<String, Channel>();
    ConcurrentHashSet<Channel> channels = new ConcurrentHashSet<Channel>();

    @Override
    public void connected(Channel channel) throws RemotingException {
        channels.add(channel);
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        channels.remove(channel);
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        channel.send(message);
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        //echo
        channel.send(message);
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        throw new RemotingException(channel, exception);

    }

    public Set<Channel> getChannels() {
        return Collections.unmodifiableSet(channels);
    }
}
