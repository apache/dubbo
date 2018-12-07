/*
 * Copyright (C) 2009-2018 Hangzhou FanDianEr Technology Co., Ltd. All rights reserved
 */
package org.apache.dubbo.rpc.cluster.router;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.Router;

/**
 * AbstractRouter
 *
 * @since 2018-12-07
 */
public abstract class AbstractRouter implements Router {
    protected int priority;
    protected URL url;

    protected AbstractRouter(int priority, URL url) {
        this.priority = priority;
        this.url = url;
    }

    protected AbstractRouter() {
    }

    @Override
    public Integer getPriority() {
        return priority;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public int compareTo(Router o) {
        if (o == null) {
            throw new IllegalArgumentException();
        }
        if (this.priority == o.getPriority()) {
            if (o.getUrl() == null) {
                return -1;
            }
            return url.toFullString().compareTo(o.getUrl().toFullString());
        } else {
            return this.priority > o.getPriority() ? 1 : -1;
        }
    }
}
