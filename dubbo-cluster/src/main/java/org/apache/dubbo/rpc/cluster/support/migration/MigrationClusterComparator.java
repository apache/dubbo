/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2020 All Rights Reserved.
 */
package org.apache.dubbo.rpc.cluster.support.migration;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invoker;

import java.util.List;

/**
 * Copyright (C) 2013-2018 Ant Financial Services Group
 *
 * @author quhongwei
 * @version : MigrationClusterComparator.java, v 0.1 2020年08月25日 20:42 quhongwei Exp $
 */
@SPI
public interface MigrationClusterComparator {

    <T> boolean compare(List<Invoker<T>>  interfaceInvokers, List<Invoker<T>>  serviceInvokers);
}