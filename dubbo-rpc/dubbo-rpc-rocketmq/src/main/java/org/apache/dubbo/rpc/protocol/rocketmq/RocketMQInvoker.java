package org.apache.dubbo.rpc.protocol.rocketmq;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLE_TIMEOUT_COUNTDOWN_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_ATTACHMENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIME_COUNTDOWN_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.threadpool.ThreadlessExecutor;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.buffer.DynamicChannelBuffer;
import org.apache.dubbo.remoting.buffer.HeapChannelBuffer;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.InvokeMode;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.protocol.dubbo.DubboCountCodec;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.RequestCallback;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;


public class RocketMQInvoker<T> extends AbstractInvoker<T> {

	
	
	private DubboCountCodec dubboCountCodec = new DubboCountCodec();
	
	private final ReentrantLock destroyLock = new ReentrantLock();

	private DefaultMQProducer defaultMQProducer;

	private final String version;
	
	private String group;
	
	private MessageQueue messageQueue;
	
	private Channel channel;

	private String topic;
	
	private String groupModel;
	
	
	
	public RocketMQInvoker(Class<T> type, URL url,RocketMQProtocolServer rocketMQProtocolServer) {
		super(type, url);
		this.version = url.getParameter(CommonConstants.VERSION_KEY);
		this.group = url.getParameter(CommonConstants.GROUP_KEY);
		this.groupModel = url.getParameter("groupModel");
		this.defaultMQProducer = rocketMQProtocolServer.getDefaultMQProducer();
		this.topic = url.getParameter("topic");
		Integer queueId = url.getParameter("queueId",Integer.class,-1);
		if( queueId != -1) {
			messageQueue = new MessageQueue();
			messageQueue.setBrokerName(url.getParameter("brokerName"));
			messageQueue.setTopic(this.topic);
			messageQueue.setQueueId(queueId);
		}
	}
	
    protected ExecutorService getCallbackExecutor(URL url, Invocation inv) {
        ExecutorService sharedExecutor = ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension().getExecutor(url);
        if (InvokeMode.SYNC == RpcUtils.getInvokeMode(getUrl(), inv)) {
            if(sharedExecutor == null) {
                sharedExecutor = ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension().getSharedExecutor();
            }
            return new ThreadlessExecutor(sharedExecutor);
        } else {
            return sharedExecutor;
        }
    }

	@Override
	protected Result doInvoke(Invocation invocation) throws Throwable {
		RpcInvocation inv = (RpcInvocation) invocation;
		final String methodName = RpcUtils.getMethodName(invocation);
		inv.setAttachment(PATH_KEY, getUrl().getPath());
		inv.setAttachment(VERSION_KEY, version);
		// direct
		try {
			RpcContext.getContext().setLocalAddress(NetUtils.getLocalHost(), 0);
			
			boolean isOneway = RpcUtils.isOneway(getUrl(), invocation);
			int timeout = calculateTimeout(invocation, methodName);
			invocation.put(TIMEOUT_KEY, timeout);
			
			Request request = new Request();
			request.setData(inv);
			// dynamic，heap，direct
			DynamicChannelBuffer buffer = new DynamicChannelBuffer(2048);
			
			dubboCountCodec.encode(channel, buffer, request);
			
			Message message  = new Message(topic, null, buffer.array());
			//message.putUserProperty(MessageConst.PROPERTY_MESSAGE_TYPE, "MixAll.REPLY_MESSAGE_FLAG");
			message.putUserProperty("consumer", NetUtils.getLocalHost());
			if(Objects.nonNull(this.groupModel) && Objects.equals(this.groupModel, "select")) {
				message.putUserProperty(CommonConstants.GENERIC_KEY, this.group);
				message.putUserProperty(CommonConstants.VERSION_KEY, this.version);
			}
			if (isOneway) {
				if(Objects.isNull(messageQueue)) {
					defaultMQProducer.sendOneway(message);
				}else {
					defaultMQProducer.sendOneway(message, messageQueue);
				}
				return AsyncRpcResult.newDefaultAsyncResult(invocation);
			} else {
				CompletableFuture<AppResponse> appResponseFuture = DefaultFuture.newFuture(channel, request, timeout, this.getCallbackExecutor(getUrl(), inv))
								.thenApply(obj -> (AppResponse) obj);
				DubboRequestCallback dubboRequestCallback = new DubboRequestCallback();
				AsyncRpcResult result = new AsyncRpcResult(appResponseFuture, inv);
				if(Objects.isNull(messageQueue)) {
					defaultMQProducer.request(message,dubboRequestCallback, timeout);
				}else {
					defaultMQProducer.request(message,messageQueue,dubboRequestCallback, timeout);
				}
				return result;
			}
		} catch (RemotingTooMuchRequestException e) {
			throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: "
					+ invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: "
					+ invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
		}
	}

	private int calculateTimeout(Invocation invocation, String methodName) {
		Object countdown = RpcContext.getContext().get(TIME_COUNTDOWN_KEY);
		int timeout = DEFAULT_TIMEOUT;
		if (countdown == null) {
			timeout = (int) RpcUtils.getTimeout(getUrl(), methodName, RpcContext.getContext(), DEFAULT_TIMEOUT);
			if (getUrl().getParameter(ENABLE_TIMEOUT_COUNTDOWN_KEY, false)) {
				invocation.setObjectAttachment(TIMEOUT_ATTACHMENT_KEY, timeout); // pass timeout to remote server
			}
		} else {
			TimeoutCountDown timeoutCountDown = (TimeoutCountDown) countdown;
			timeout = (int) timeoutCountDown.timeRemaining(TimeUnit.MILLISECONDS);
			invocation.setObjectAttachment(TIMEOUT_ATTACHMENT_KEY, timeout);// pass timeout to remote server
		}
		return timeout;
	}

	@Override
	public boolean isAvailable() {
		if (!super.isAvailable()) {
			return false;
		}
		return true;
	}

	public void destroy() {
		if (super.isDestroyed()) {
			return;
		}
		try {
			destroyLock.lock();
			if (super.isDestroyed()) {
				return;
			}
			defaultMQProducer.shutdown();
		} finally {
			destroyLock.unlock();
		}
	}
	
	class DubboRequestCallback implements RequestCallback{
		@Override
		public void onSuccess(Message message) {
			try {
				HeapChannelBuffer heapChannelBuffer = new HeapChannelBuffer(message.getBody());
				Response response = (Response)dubboCountCodec.decode(channel, heapChannelBuffer);
				DefaultFuture.received(channel, response);
			} catch (Exception e) {
				this.onException(e);
			}
		}

		@Override
		public void onException(Throwable e) {
			Response response = new Response();
			response.setErrorMessage(e.getMessage());
			response.setStatus(Response.SERVICE_ERROR);
			DefaultFuture.received(channel, response);
			logger.error(e.getMessage(), e);
		}
	}
}
