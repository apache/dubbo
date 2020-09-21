/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2020 All Rights Reserved.
 */
package org.apache.dubbo.rpc.cluster.support.migration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Copyright (C) 2013-2018 Ant Financial Services Group
 *
 * @author quhongwei
 * @version : MigrationCluserInvoker.java, v 0.1 2020年08月25日 20:19 quhongwei Exp $
 */
public interface MigrationClusterInvoker<T> extends ClusterInvoker<T> {

    boolean isServiceInvoker();

    MigrationRule getMigrationRule();

    void setMigrationRule(MigrationRule rule);

    void destroyServiceDiscoveryInvoker(ClusterInvoker<?> invoker);

    void discardServiceDiscoveryInvokerAddress(ClusterInvoker<?> invoker);

    void discardInterfaceInvokerAddress(ClusterInvoker<T> invoker);

    void refreshServiceDiscoveryInvoker();

    void refreshInterfaceInvoker();

    void destroyInterfaceInvoker(ClusterInvoker<T> invoker);

    boolean isMigrationMultiRegsitry();

    void migrateToServiceDiscoveryInvoker(boolean forceMigrate);

    void reRefer(URL newSubscribeUrl);

    void fallbackToInterfaceInvoker();

    AtomicBoolean invokersChanged();

}