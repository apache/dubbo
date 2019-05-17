/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.remoting.etcd;

import static org.apache.dubbo.remoting.Constants.DEFAULT_IO_THREADS;

public interface Constants {
    String ETCD3_NOTIFY_MAXTHREADS_KEYS = "etcd3.notify.maxthreads";

    int DEFAULT_ETCD3_NOTIFY_THREADS = DEFAULT_IO_THREADS;

    String DEFAULT_ETCD3_NOTIFY_QUEUES_KEY = "etcd3.notify.queues";

    int DEFAULT_GRPC_QUEUES = 300_0000;
}

