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

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.common.threadpool.serial.SerializingExecutor;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.concurrent.Executor;

/**
 * An abstract stream implementation.
 */
public abstract class AbstractStream implements Stream {

    protected Executor executor;
    protected final FrameworkModel frameworkModel;


    private static final boolean HAS_PROTOBUF = hasProtobuf();

    public AbstractStream(Executor executor, FrameworkModel frameworkModel) {
        this.executor = new SerializingExecutor(executor);
        this.frameworkModel = frameworkModel;
    }

    public void setExecutor(Executor executor) {
        this.executor = new SerializingExecutor(executor);
    }

    public static boolean getGrpcStatusDetailEnabled() {
        return HAS_PROTOBUF;
    }

    private static boolean hasProtobuf() {
        try {
            ClassUtils.forName("com.google.protobuf.Message");
            return true;
        } catch (ClassNotFoundException ignore) {
            return false;
        }
    }
}
