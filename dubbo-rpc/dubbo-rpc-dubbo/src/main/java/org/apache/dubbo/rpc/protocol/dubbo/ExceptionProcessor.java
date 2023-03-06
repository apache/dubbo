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

package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Decodeable;
import org.apache.dubbo.remoting.exchange.Request;

import java.io.InputStream;

import static org.apache.dubbo.common.extension.ExtensionScope.FRAMEWORK;

/**
 * <p>Interface for custom decoding logic in specific spi-extended classes.
 *
 * <p>Mainly works in decode phase {@link Decodeable#decode()},
 * and can only handle exception types in this phase.
 *
 *
 * @since 3.2.0
 */
@SPI(scope = FRAMEWORK)
public interface ExceptionProcessor {

    /**
     *  Get an enhanced DecodeableRpcInvocation subclass to allow custom decode.
     *  The parameters are the same as {@link DecodeableRpcInvocation}
     */
    DecodeableRpcInvocation getRetryDecodeableRpcInvocation(Channel channel, Request req, InputStream is, byte proto);
}
