package org.apache.dubbo.registry.nameservice;

import static org.apache.dubbo.registry.Constants.ADMIN_PROTOCOL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.impl.MQClientManager;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;

public class NameServiceRegistry extends FailbackRegistry {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private ScheduledExecutorService scheduledExecutorService;

	private Map<URL, RegistryInfoWrapper> consumerRegistryInfoWrapperMap = new ConcurrentHashMap<>();

	private MQClientInstance client;

	private boolean isNotRoute = true;

	private ClusterInfo clusterInfo;

	private TopicList topicList;

	private long timeoutMillis;
	
	private String nameservAddr;
	
	private Integer nameservPort;
	
	private String groupModel;

	public NameServiceRegistry(URL url) {
		super(url);
		this.nameservAddr = url.getHost();
		this.nameservPort = url.getPort();
		this.isNotRoute = url.getParameter("route", true);
		if (this.isNotRoute) {
			this.groupModel = url.getParameter("groupModel", "select");
			this.timeoutMillis = url.getParameter("timeoutMillis", 3000);

			ClientConfig clientConfig = new ClientConfig();
			clientConfig.setNamesrvAddr( this.nameservAddr );
			client = MQClientManager.getInstance().getOrCreateMQClientInstance(clientConfig);
			try {
				this.initBeasInfo();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "dubbo-registry-nameservice");
				}
			});
			scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					try {
						NameServiceRegistry.this.initBeasInfo();

						if (consumerRegistryInfoWrapperMap.isEmpty()) {
							return;
						}
						for (Entry<URL, RegistryInfoWrapper> e : consumerRegistryInfoWrapperMap.entrySet()) {
							List<URL> urls = new ArrayList<URL>();
							NameServiceRegistry.this.pullRoute(e.getValue().serviceName, e.getKey(), urls);
							e.getValue().listener.notify(urls);
						}
					} catch (Exception e) {
						logger.error("ScheduledTask pullRoute exception", e);
					}
				}
			}, 1000 * 10, 3000 * 10, TimeUnit.MILLISECONDS);
		} 
	}

	private void initBeasInfo() throws Exception {
		this.clusterInfo = this.client.getMQClientAPIImpl().getBrokerClusterInfo(timeoutMillis);
		this.topicList = this.client.getMQClientAPIImpl().getTopicListFromNameServer(timeoutMillis);
	}

	private URL createProviderURL(ServiceName serviceName, URL url, int queue) {
		URL providerURL = new URL("rocketmq", this.nameservAddr, this.nameservPort, serviceName.getServiceInterface());
		providerURL.addParameters(url.getParameters());

		providerURL.addParameter(CommonConstants.INTERFACE_KEY, serviceName.getServiceInterface());
		providerURL.addParameter(CommonConstants.PATH_KEY, serviceName.getServiceInterface());
		providerURL.addParameter("bean.name", "ServiceBean:" + serviceName.getServiceInterface());
		providerURL.addParameter(CommonConstants.SIDE_KEY, CommonConstants.PROVIDER);
		providerURL.addParameter(RegistryConstants.CATEGORY_KEY, "providers");
		providerURL.addParameter(CommonConstants.PROTOCOL_KEY, "rocketmq");
		providerURL.addParameter("queueId", queue);
		providerURL.addParameter("topic", serviceName.getValue());
		providerURL.addParameter("groupModel", this.groupModel);
		return providerURL;
	}
	
	private ServiceName createServiceName(URL url) {
		return new ServiceName(url,this.groupModel);
	}

	private boolean isAdminProtocol(URL url) {
		return ADMIN_PROTOCOL.equals(url.getProtocol());
	}

	private boolean createTopic(ServiceName serviceName) {
		if (!this.topicList.getTopicList().contains(serviceName.getValue())) {
			for (Entry<String, BrokerData> entry : this.clusterInfo.getBrokerAddrTable().entrySet()) {
				String brokerArr = entry.getValue().getBrokerAddrs().get(MixAll.MASTER_ID);
				try {
					TopicConfig topicConfig = new TopicConfig(serviceName.getValue());
					topicConfig.setReadQueueNums(8);
					topicConfig.setWriteQueueNums(8);
					this.client.getMQClientAPIImpl().createTopic(brokerArr, null, topicConfig, timeoutMillis);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
			return true;
		} else {
			return false;
		}

	}

	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public void doRegister(URL url) {
		ServiceName serviceName = this.createServiceName(url);
		this.createTopic(serviceName);
		url.addParameter("namesrv", this.nameservAddr+":"+this.nameservPort);
		url.addParameter("topic", serviceName.getValue());
		url.addParameter("groupModel", this.groupModel);
	}

	@Override
	public void doUnregister(URL url) {

	}

	@Override
	public void doSubscribe(URL url, NotifyListener listener) {
		List<URL> urls = new ArrayList<URL>();
		ServiceName serviceName = this.createServiceName(url);
		if (this.isNotRoute) {
			URL providerURL = this.createProviderURL(serviceName, url, -1);
			urls.add(providerURL);
		} else {
			this.pullRoute(serviceName, url, urls);
		}
		listener.notify(urls);
	}

	void pullRoute(ServiceName serviceName, URL url, List<URL> urls) {
		try {
			this.createTopic(serviceName);
			String topic = serviceName.getValue();
			TopicRouteData topicRouteData = this.client.getMQClientAPIImpl().getTopicRouteInfoFromNameServer(topic,
					this.timeoutMillis);

			Map<String, String> brokerAddrBybrokerName = new HashMap<>();
			for (BrokerData brokerData : topicRouteData.getBrokerDatas()) {
				brokerAddrBybrokerName.put(brokerData.getBrokerName(), brokerData.selectBrokerAddr());
			}
			for (QueueData queueData : topicRouteData.getQueueDatas()) {
				if (PermName.isReadable(queueData.getPerm())) {
					for (int i = 0; i < queueData.getReadQueueNums(); i++) {
						URL newUrl = this.createProviderURL(serviceName, url, i);
						newUrl.addParameter("brokerName", queueData.getBrokerName());
						urls.add(newUrl);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doUnsubscribe(URL url, NotifyListener listener) {
		this.consumerRegistryInfoWrapperMap.remove(url);
	}

	private class RegistryInfoWrapper {

		private URL url;

		private NotifyListener listener;

		private ServiceName serviceName;
	}
}
