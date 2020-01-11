package org.apache.dubbo.registry.pilot;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.registry.Constants;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.remoting.pilot.ChildListener;
import org.apache.dubbo.remoting.pilot.PilotClient;
import org.apache.dubbo.remoting.pilot.PilotTransporter;
import org.apache.dubbo.remoting.pilot.StateListener;
import org.apache.dubbo.rpc.RpcException;

import java.util.*;
import java.util.concurrent.*;

/**
 * Support for pilot registry
 * @author hzj
 * @date 2019/03/20
 */
public class PilotRegistry extends FailbackRegistry {

    private final PilotClient pilotClient;
    private final long notifyWaitTime;
    private final int reconnectDelayTime;
    private Map<NotifyListener, ChildListener> pilotListeners = new ConcurrentHashMap<>();
    private ScheduledExecutorService reconnectSchedule;

    public PilotRegistry(URL url, PilotTransporter pilotTransporter) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("Pilot registry address is invalid, url: " + url);
        }

        pilotClient = pilotTransporter.connect(url);
        reconnectDelayTime = url.getParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RETRY_PERIOD);
        pilotClient.addStateListener(state -> {
            if (state == StateListener.CONNECTED) {
                reconnectSchedule.schedule(() -> {
                    try {
                        if (pilotClient.isConnected()) {
                            pilotClient.closeOldStream();
                            pilotListeners.clear();
                            recoverSubscribed();
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }, new Random().nextInt(reconnectDelayTime), TimeUnit.MILLISECONDS);
            }
        });
        reconnectSchedule = Executors.newScheduledThreadPool(1,
                new NamedThreadFactory("pilot-reconnect-executor"));
        notifyWaitTime = url.getParameter(CommonConstants.TIMEOUT_KEY, DEFUALT_PILOT_SUBSCRIBE_WAIT_TIME);
    }

    @Override
    public void doRegister(URL url) {
        throw new UnsupportedOperationException("Pilot registry don't support register operation");
    }

    @Override
    public void doUnregister(URL url) {
        throw new UnsupportedOperationException("Pilot registry don't support unRegister operation");
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        try {
            String service = url.getServiceInterface();
            CountDownLatch notified = new CountDownLatch(1);
            ChildListener childListener =
                    Optional.ofNullable(pilotListeners.get(listener))
                            .orElseGet(() -> {
                                ChildListener watchListener, prev;
                                prev = pilotListeners.putIfAbsent(listener, watchListener =
                                        (path, urls) -> {
                                            PilotRegistry.this.notify(url, listener,
                                                    PilotRegistry.this.toUrlsWithEmpty(url, path, urls));
                                            notified.countDown();
                                        });
                                return prev != null ? prev : watchListener;
                            });
            if (!pilotClient.isConnected()) {
                throw new RpcException("Pilot is not connected");
            }
            this.pilotClient.addChildListener(service, childListener);
            pilotClient.discoveryService(url);
            //wait for first watch result
            if (!notified.await(notifyWaitTime, TimeUnit.MILLISECONDS)) {
                pilotListeners.remove(listener);
                this.pilotClient.removeChildListener(service, childListener);
                throw new RpcException("Receive none discovery response from pilot in " + notifyWaitTime + " ms");
            }
        } catch (Throwable e) {
            throw new RpcException("Failed to subscribe " + url + " from pilot registry: " + getUrl() + " ,cause: " + e.getMessage(), e);
        }
    }

    private List<URL> toUrlsWithEmpty(URL url, String path, List<URL> urls) {
        if (urls == null || urls.isEmpty()) {
            URL empty = url.setProtocol(RegistryConstants.EMPTY_PROTOCOL).addParameter(RegistryConstants.CATEGORY_KEY, RegistryConstants.PROVIDERS_CATEGORY);
            Objects.requireNonNull(urls).add(empty);
        }
        return urls;
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        //doNothing
    }

    @Override
    public boolean isAvailable() {
        return pilotClient.isConnected();
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            pilotClient.close();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    public static final int DEFUALT_PILOT_SUBSCRIBE_WAIT_TIME = 10 * 1000;
}
