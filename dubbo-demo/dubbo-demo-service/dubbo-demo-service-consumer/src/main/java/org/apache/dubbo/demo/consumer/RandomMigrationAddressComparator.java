/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2020 All Rights Reserved.
 */
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationClusterComparator;

import java.util.List;
import java.util.Random;

/**
 * Copyright (C) 2013-2018 Ant Financial Services Group
 *
 * @author quhongwei
 * @version : RandomMigrationAddressComparator.java, v 0.1 2020年09月02日 17:14 quhongwei Exp $
 */
public class RandomMigrationAddressComparator implements MigrationClusterComparator {
    @Override
    public <T> boolean shouldMigrate(List<Invoker<T>> interfaceInvokers, List<Invoker<T>> serviceInvokers) {
        boolean ret = ((int)(Math.random() * 100)) % 2 == 0;
        System.out.println("RandomMigrationAddressComparator ret =" + ret);
        return ret;
    }
}