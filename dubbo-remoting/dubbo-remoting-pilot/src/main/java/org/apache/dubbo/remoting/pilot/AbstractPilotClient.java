package org.apache.dubbo.remoting.pilot;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * abstract grpc support for pilot client
 * @author hzj
 * @date 2019/03/20
 */
public abstract class AbstractPilotClient implements PilotClient {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractPilotClient.class);
    protected final URL url;
    private final List<StateListener> stateListeners = new ArrayList<>();
    private final Map<String, Set<ChildListener>> childListeners = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    public AbstractPilotClient(URL url) {
        this.url = url;
    }

    @Override
    public void discoveryService(URL url) { doDiscoveryService(url);}

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        stateListeners.clear();
        try {
            doClose();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    @Override
    public void closeOldStream() {
        try {
            childListeners.clear();
            doCloseOldStream();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    protected void stateChanged(int state) {
        for (StateListener sessionListener : getStateListeners()) {
            sessionListener.stateChanged(state);
        }
    }

    @Override
    public void addStateListener(StateListener listener) {
        stateListeners.add(listener);
    }

    @Override
    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
    }

    @Override
    public List<StateListener> getStateListeners() {
        return stateListeners;
    }

    @Override
    public Set<ChildListener> addChildListener(String path, ChildListener listener) {
        Set<ChildListener> listeners = childListeners.get(path);
        if (listeners == null) {
            childListeners.putIfAbsent(path, new HashSet<>());
            listeners = childListeners.get(path);
        }
        listeners.add(listener);
        return listeners;
    }

    @Override
    public void removeChildListener(String path, ChildListener listener) {
        Set<ChildListener> listeners = childListeners.get(path);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public Set<ChildListener> getChildListeners(String path) {
        Set<ChildListener> listeners = childListeners.get(path);
        if (listeners == null) {
            childListeners.putIfAbsent(path, new HashSet<>());
            listeners = childListeners.get(path);
        }
        return listeners;
    }

    public void responseReceived(String path, List<URL> urls) {
        for (ChildListener childListener : getChildListeners(path)) {
            childListener.chiledChanged(path, urls);
        }
    }

    abstract public void doDiscoveryService(URL url);

    abstract public void doClose();

    abstract public void doCloseOldStream();
}
