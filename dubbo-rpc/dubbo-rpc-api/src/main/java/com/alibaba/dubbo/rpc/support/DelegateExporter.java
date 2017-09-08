/**
 * Project: dubbo-rpc
 * <p>
 * File Created at 2012-2-24
 * $Id$
 * <p>
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.rpc.support;

import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;

/**
 * DelegateExporter
 * @author chao.liuc
 *
 */
public class DelegateExporter<T> implements Exporter<T> {

    private final Exporter<T> exporter;

    public DelegateExporter(Exporter<T> exporter) {
        if (exporter == null) {
            throw new IllegalArgumentException("exporter can not be null");
        } else {
            this.exporter = exporter;
        }

    }

    public Invoker<T> getInvoker() {
        return exporter.getInvoker();
    }

    public void unexport() {
        exporter.unexport();
    }
}
