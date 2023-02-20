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

package org.apache.dubbo.remoting;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.Request;

import java.util.Map;

import static org.apache.dubbo.common.extension.ExtensionScope.FRAMEWORK;

/**
 * <p>Interface for handling exceptions in specific spi-extended classes.
 *
 * <p>Mainly works in the decode phase {@link Decodeable#decode()},
 * and can only handle exception types in this phase.
 *
 * <p>Allow three kinds of customized operations, customize abnormal results and return,
 * customize normal results and return, interrupt and retry the process.
 *
 * @since 3.2.0
 */
@SPI(scope = FRAMEWORK)
public interface ExceptionProcessor {

    /**
     * Whether to custom handle error when an exception
     * with the specified parameter is encountered
     *
     * @param throwable specified exception
     */
    boolean shouldHandleError(Throwable throwable);

    /**
     * Determined to execute by {@link ExceptionProcessor#shouldHandleError(Throwable)}}
     * <p>When encountering an exception in decode phase, allow developer customize the behavior before return.
     * <p>If exceptions are allowed and want to reprocess the request,
     * you can add custom content and throw a retry exception{@link RetryHandleException} so that decode will continue processing.
     * <p>Decode will not re-read the read content, so you need to save the decoded content, refer to default impl {snf}.
     * <p>The number of times decode handle exceptions will not exceed 2.
     * <p>If decode still cannot process the request, the error message of retry exception will be returned eventually,
     * and the real reason that cannot be processed needs to be set instead of the literal exception information of retry.
     *
     * @return Msg information, If non-null, means that the exception is customized
     */
    String wrapAndHandleException(ExchangeChannel channel, Request req) throws RetryHandleException;


    /**
     * Custom handling of parameter types that can preserve the original parameter type
     *
     * @param pts The actual parameters passed by consumer
     */
    void customPts(Object context, Class<?>[] pts);

    /**
     * Custom handling of attachment
     *
     * @param map The actual attachment passed by consumer
     */
    void customAttachment(Object context, Map<String, Object> map);

    /**
     * release resources
     */
    void cleanUp(Object context);

}
