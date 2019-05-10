package org.apache.dubbo.remoting.etcd;

import static org.apache.dubbo.common.Constants.DEFAULT_IO_THREADS;

public interface Constants {
    String ETCD3_NOTIFY_MAXTHREADS_KEYS = "etcd3.notify.maxthreads";

    int DEFAULT_ETCD3_NOTIFY_THREADS = DEFAULT_IO_THREADS;

    String DEFAULT_ETCD3_NOTIFY_QUEUES_KEY = "etcd3.notify.queues";

    int DEFAULT_GRPC_QUEUES = 300_0000;
}

