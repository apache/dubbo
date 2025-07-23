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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.remoting.http12.message.DefaultHttpResult.Builder;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.TriRpcStatus.Code;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.h12.grpc.GrpcHeaderNames;

public final class ExceptionCustomizerWrapper {

    private final CompositeExceptionHandler exceptionHandler;

    private RequestMetadata metadata;
    private MethodDescriptor methodDescriptor;
    private boolean needWrap;

    public ExceptionCustomizerWrapper(FrameworkModel frameworkModel) {
        exceptionHandler = frameworkModel.getOrRegisterBean(CompositeExceptionHandler.class);
    }

    public void setMetadata(RequestMetadata metadata) {
        this.metadata = metadata;
    }

    public void setMethodDescriptor(MethodDescriptor methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    public void setNeedWrap(boolean needWrap) {
        this.needWrap = needWrap;
    }

    public Level resolveLogLevel(Throwable throwable) {
        return exceptionHandler.resolveLogLevel(throwable);
    }

    public void customizeGrpcStatus(HttpHeaders headers, Throwable throwable) {
        if (throwable == null) {
            headers.set(GrpcHeaderNames.GRPC_STATUS.getName(), "0");
            return;
        }
        exceptionHandler.resolveGrpcStatus(throwable, headers, metadata, methodDescriptor);
    }

    public Object customize(Throwable throwable) {
        return exceptionHandler.handle(throwable, metadata, methodDescriptor);
    }

    @SuppressWarnings("unchecked")
    public Object customizeGrpc(Throwable throwable) {
        Object result = exceptionHandler.handleGrpc(throwable, metadata, methodDescriptor);
        if (needWrap) {
            Builder<Object> builder = null;
            if (result == null) {
                builder = HttpResult.builder().body(throwable);
            } else if (result instanceof Throwable) {
                builder = HttpResult.builder().body(result);
            } else if (result instanceof HttpResult) {
                HttpResult<Object> httpResult = (HttpResult<Object>) result;
                if (httpResult.getBody() instanceof Throwable) {
                    builder = HttpResult.builder().from(httpResult);
                }
            }
            if (builder == null) {
                return result;
            }
            Code code = TriRpcStatus.getStatus(throwable).code;
            return builder.header(
                            TripleHeaderEnum.TRI_EXCEPTION_CODE.getName(),
                            code == TriRpcStatus.UNKNOWN.code ? RpcException.BIZ_EXCEPTION : code.code)
                    .build();
        }
        return result instanceof Throwable ? result : null;
    }
}
