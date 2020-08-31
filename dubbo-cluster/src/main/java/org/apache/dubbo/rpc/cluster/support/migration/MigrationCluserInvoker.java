/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2020 All Rights Reserved.
 */
package org.apache.dubbo.rpc.cluster.support.migration;

import org.apache.dubbo.rpc.cluster.ClusterInvoker;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Copyright (C) 2013-2018 Ant Financial Services Group
 *
 * @author quhongwei
 * @version : MigrationCluserInvoker.java, v 0.1 2020年08月25日 20:19 quhongwei Exp $
 */
public interface MigrationCluserInvoker<T> extends ClusterInvoker<T> {

    AtomicBoolean addressChanged();

    boolean isServiceInvoker();

    MigrationRule getMigrationRule();

    void setMigrationRule(MigrationRule rule);

    void destroyServiceDiscoveryInvoker();

    void discardServiceDiscoveryInvokerAddress();

    void discardInterfaceInvokerAddress();

    void refreshServiceDiscoveryInvoker();

    void refreshInterfaceInvoker();

    void destroyInterfaceInvoker();
}