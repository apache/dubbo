package org.apache.dubbp.rpc.rocketmq;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

public class RocketMQChannel implements Channel {

	private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	@Override
	public URL getUrl() {
		return null;
	}

	@Override
	public ChannelHandler getChannelHandler() {
		return null;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return null;
	}

	@Override
	public void send(Object message) throws RemotingException {

	}

	@Override
	public void send(Object message, boolean sent) throws RemotingException {

	}

	@Override
	public void close() {

	}

	@Override
	public void close(int timeout) {

	}

	@Override
	public void startClose() {

	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return null;
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public boolean hasAttribute(String key) {
		return attributes.containsKey(key);
	}

	@Override
	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	@Override
	public void setAttribute(String key, Object value) {
		// The null value is unallowed in the ConcurrentHashMap.
		if (value == null) {
			attributes.remove(key);
		} else {
			attributes.put(key, value);
		}
	}

	@Override
	public void removeAttribute(String key) {
		attributes.remove(key);
	}




}
