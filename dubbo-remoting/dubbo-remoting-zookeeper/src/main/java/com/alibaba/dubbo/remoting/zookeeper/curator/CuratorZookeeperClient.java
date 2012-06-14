package com.alibaba.dubbo.remoting.zookeeper.curator;

import java.io.IOException;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.zookeeper.ChildListener;
import com.alibaba.dubbo.remoting.zookeeper.StateListener;
import com.alibaba.dubbo.remoting.zookeeper.support.AbstractZookeeperClient;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.framework.CuratorFrameworkFactory.Builder;
import com.netflix.curator.framework.api.BackgroundCallback;
import com.netflix.curator.framework.api.CuratorEvent;
import com.netflix.curator.framework.state.ConnectionState;
import com.netflix.curator.framework.state.ConnectionStateListener;
import com.netflix.curator.retry.RetryNTimes;

public class CuratorZookeeperClient extends AbstractZookeeperClient<BackgroundCallback> {

	private final CuratorFramework client;

	public CuratorZookeeperClient(URL url) {
		super(url);
		try {
			Builder builder = CuratorFrameworkFactory.builder()
					.connectString(url.getBackupAddress())
			        .retryPolicy(new RetryNTimes(Integer.MAX_VALUE, 1000))  
			        .connectionTimeoutMs(5000);
			String authority = url.getAuthority();
			if (authority != null && authority.length() > 0) {
				builder = builder.authorization("digest", authority.getBytes());
			}
			client = builder.build();
			client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
				public void stateChanged(CuratorFramework client, ConnectionState state) {
					if (state == ConnectionState.LOST) {
						CuratorZookeeperClient.this.stateChanged(StateListener.DISCONNECTED);
					} else if (state == ConnectionState.CONNECTED) {
						CuratorZookeeperClient.this.stateChanged(StateListener.CONNECTED);
					} else if (state == ConnectionState.RECONNECTED) {
						CuratorZookeeperClient.this.stateChanged(StateListener.RECONNECTED);
					}
				}
			});
			client.start();
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public void createPersistent(String path) {
		try {
			client.create().forPath(path);
		} catch (NodeExistsException e) {
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public void createEphemeral(String path) {
		try {
			client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
		} catch (NodeExistsException e) {
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public void delete(String path) {
		try {
			client.delete().forPath(path);
		} catch (NoNodeException e) {
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public List<String> getChildren(String path) {
		try {
			return client.getChildren().forPath(path);
		} catch (NoNodeException e) {
			return null;
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public boolean isConnected() {
		return client.getZookeeperClient().isConnected();
	}

	public void close() {
		client.close();
	}
	
	public BackgroundCallback createTargetChildListener(String path, final ChildListener listener) {
		return new BackgroundCallback() {
			public void processResult(CuratorFramework client, CuratorEvent event)
					throws Exception {
				if (listener != null) {
					listener.childChanged(event.getPath(), event.getChildren());
				}
			}
		};
	}

	public List<String> addTargetChildListener(String path, final BackgroundCallback listener) {
		try {
			return client.getChildren().inBackground(listener).forPath(path);
		} catch (NoNodeException e) {
			return null;
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public void removeTargetChildListener(String path, BackgroundCallback listener) {
		// TODO remove listener
	}

}
