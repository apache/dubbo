
package org.apache.dubbp.rpc.rocketmq;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;

import java.util.List;
import java.util.Objects;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.DynamicChannelBuffer;
import org.apache.dubbo.remoting.buffer.HeapChannelBuffer;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.protocol.dubbo.DubboCountCodec;
import org.apache.rocketmq.client.common.ClientErrorCode;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.utils.MessageUtil;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;


public class RocketMQProtocol extends AbstractProtocol {

	public static final String NAME = "rocketmq";

	public static final int DEFAULT_PORT = 20880;
	private static final String IS_CALLBACK_SERVICE_INVOKE = "_isCallBackServiceInvoke";
	private static Object MONITOR = new Object();

	private static RocketMQProtocol INSTANCE;
	
	private MessageListenerConcurrently  messageListenerConcurrently = new DubboMessageListenerConcurrently();

	public static RocketMQProtocol getRocketMQProtocol() {
		if (null == INSTANCE) {
			synchronized (MONITOR) {
				if (null == INSTANCE) {
					INSTANCE = (RocketMQProtocol) ExtensionLoader.getExtensionLoader(Protocol.class)
							.getOriginalInstance(RocketMQProtocol.NAME);
				}
			}
		}
		return INSTANCE;
	}

	
	
	public RocketMQProtocol() {
	}

	/**
	 * <host:port,Exchanger>
	 */

	@Override
	public int getDefaultPort() {
		return 0;
	}

	@Override
	public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
		URL url = invoker.getUrl();
		String topic = url.getParameter("topic");
		RocketMQExporter<T> exporter = new RocketMQExporter<T>(invoker, topic, exporterMap);
		exporterMap.addExportMap(topic, exporter);
		RocketMQProtocolServer rocketMQProtocolServer = this.openServer(url,CommonConstants.PROVIDER);
		try {
			String groupModel = url.getParameter("groupModel");
			if(Objects.nonNull(groupModel) && Objects.equals(groupModel, "select")) {
				if( Objects.isNull(url.getParameter(CommonConstants.GROUP_KEY)) &&
							Objects.isNull((url.getParameter(CommonConstants.GROUP_KEY)))) {
					// error
				}
				StringBuffer stringBuffer = new StringBuffer();
				boolean isGroup = false;
				if(Objects.nonNull(url.getParameter(CommonConstants.GROUP_KEY))) {
					stringBuffer.append(CommonConstants.GROUP_KEY).append("=").append(url.getParameter(CommonConstants.GROUP_KEY));
					isGroup = true;
				}
				if(Objects.nonNull(url.getParameter(CommonConstants.VERSION_KEY))) {
					if(isGroup) {
						stringBuffer.append(" and ");
					}
					stringBuffer.append(CommonConstants.VERSION_KEY).append("=").append(url.getParameter(CommonConstants.VERSION_KEY));
				}
				MessageSelector messageSelector = MessageSelector.bySql(stringBuffer.toString());
				rocketMQProtocolServer.getDefaultMQPushConsumer().subscribe(topic, messageSelector);
			}else {
				rocketMQProtocolServer.getDefaultMQPushConsumer().subscribe(topic,CommonConstants.ANY_VALUE);
			}
			return exporter;
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private RocketMQProtocolServer openServer(URL url,String model) {
		// find server.
		String key = url.getAddress() ;
		ProtocolServer server = serverMap.get(key);
		if (server == null) {
			synchronized (this) {
				server = serverMap.get(key);
				if (server == null) {
					serverMap.put(key, createServer(url,key,model));
				}
				server = serverMap.get(key);
				
				RocketMQProtocolServer rocketMQProtocolServer = (RocketMQProtocolServer)server;
				rocketMQProtocolServer.setModel(model);
				rocketMQProtocolServer.setMessageListenerConcurrently(this.messageListenerConcurrently);
				return rocketMQProtocolServer;
			}
		} else {
			server.reset(url);
			return (RocketMQProtocolServer)server;
		}
	}
	
	 private ProtocolServer createServer(URL url,String key,String model) {
		 RocketMQProtocolServer rocketMQProtocolServer = new RocketMQProtocolServer();
		 rocketMQProtocolServer.setModel(model);
		 rocketMQProtocolServer.reset(url);
		 return rocketMQProtocolServer;
	 }

	@Override
	protected <T> Invoker<T> protocolBindingRefer(Class<T> type, URL url) throws RpcException {
		RocketMQProtocolServer rocketMQProtocolServer = this.openServer(url,  CommonConstants.CONSUMER);
		RocketMQInvoker<T> rocketMQInvoker = new RocketMQInvoker<>(type, url, rocketMQProtocolServer);
		return rocketMQInvoker;
	}

	private class DubboMessageListenerConcurrently implements MessageListenerConcurrently {

		private DubboCountCodec dubboCountCodec = new DubboCountCodec();

		private Channel channel;

		private DefaultMQProducer defaultMQProducer;

		@Override
		public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {

			for (MessageExt messageExt : msgs) {
				Response response = new Response();
				try {
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("reply message ext is : %s", messageExt));
					}
					if(Objects.isNull(messageExt.getProperty(MessageConst.PROPERTY_CLUSTER))) {
						MQClientException exception = new MQClientException(ClientErrorCode.CREATE_REPLY_MESSAGE_EXCEPTION, "create reply message fail, requestMessage error, property[" + MessageConst.PROPERTY_CLUSTER + "] is null.");
						response.setErrorMessage(exception.getMessage());
						response.setStatus(Response.BAD_REQUEST);
						logger.error(exception);
					}else {
						HeapChannelBuffer heapChannelBuffer = new HeapChannelBuffer(messageExt.getBody());
						Object object = dubboCountCodec.decode(channel, heapChannelBuffer);
	
						if (!(object instanceof Invocation)) {
							RemotingException exception = new RemotingException("Unsupported request: "
									+ (object == null ? null : (object.getClass().getName() + ": " + object))
									+ ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: "
									+ channel.getLocalAddress());
							logger.error(exception);
							response.setErrorMessage(exception.getMessage());
							response.setStatus(Response.BAD_REQUEST);
						}
						String topic = messageExt.getTopic();
						Invocation inv = (Invocation) object;
						Invoker<?> invoker = exporterMap.getExport(topic).getInvoker();
						// need to consider backward-compatibility if it's a callback
						if (Boolean.TRUE.toString().equals(inv.getObjectAttachments().get(IS_CALLBACK_SERVICE_INVOKE))) {
							String methodsStr = invoker.getUrl().getParameters().get(METHODS_KEY);
							boolean hasMethod = false;
							if (methodsStr == null || !methodsStr.contains(COMMA_SEPARATOR)) {
								hasMethod = inv.getMethodName().equals(methodsStr);
							} else {
								String[] methods = methodsStr.split(COMMA_SEPARATOR);
								for (String method : methods) {
									if (inv.getMethodName().equals(method)) {
										hasMethod = true;
										break;
									}
								}
							}
							if (!hasMethod) {
								logger.warn(new IllegalStateException("The methodName " + inv.getMethodName()
										+ " not found in callback service interface ,invoke will be ignored."
										+ " please update the api interface. url is:" + invoker.getUrl())
										+ " ,invocation is :" + inv);
								return null;
							}
						}
						RpcContext.getContext().setRemoteAddress(channel.getRemoteAddress());
						Result result = invoker.invoke(inv);
						response.setStatus(Response.OK);
						response.setResult(result);
					}
				} catch (Exception e) {
					response.setErrorMessage(e.getMessage());
					response.setStatus(Response.BAD_REQUEST);
					logger.error(e);
					
				}
				ChannelBuffer buffer = new DynamicChannelBuffer(2048);
				try {
					dubboCountCodec.encode(channel, buffer, response);
				}catch (Exception e) {
					response.setErrorMessage(e.getMessage());
					response.setStatus(Response.BAD_REQUEST);
					logger.error(e);
				}
				try {
					SendResult sendResult = defaultMQProducer.send(MessageUtil.createReplyMessage(messageExt, buffer.array()),3000);
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("send result is : %s", sendResult));
					}
				}catch (Exception e) {
					logger.error(e);
				}
			}
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		}

	}

}
