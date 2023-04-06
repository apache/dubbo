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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.DeadlineFuture;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;

import java.util.Map;

public class UnaryClientCallListener implements ClientCall.Listener {

    private final DeadlineFuture future;
    private Object appResponse;

    public UnaryClientCallListener(DeadlineFuture deadlineFuture) {
        this.future = deadlineFuture;
    }

    @Override
    public void onMessage(Object message) {
        this.appResponse = message;
    }

    @Override
    public void onClose(TriRpcStatus status, Map<String, Object> trailers, Map<String, String> assemblyMap) {
        AppResponse result = new AppResponse();
        result.setObjectAttachments(trailers);
        if (status.isOk()) {
           Integer exceptionCode = extractExceptionCode(assemblyMap);
            if (!exceptionCode.equals(CommonConstants.TRI_EXCEPTION_CODE_NOT_EXISTS)) {
                result.setException((Exception) appResponse);
            } else {
                result.setValue(appResponse);
            }
         } else {
            result.setException(status.asException());
        }
        future.received(status, result);
    }

    private int extractExceptionCode(Map<String, String> trailers) {
        int triExceptionCode = CommonConstants.TRI_EXCEPTION_CODE_NOT_EXISTS;
        if (!(appResponse instanceof Exception)) {
            return triExceptionCode;
        }
        String exceptionCodeStr = trailers.get(TripleHeaderEnum.TRI_EXCEPTION_CODE.getHeader());
        if (!StringUtils.isEmpty(exceptionCodeStr)) {
            return Integer.parseInt(exceptionCodeStr);
        }
        return CommonConstants.TRI_EXCEPTION_CODE_NOT_EXISTS;
    }

    @Override
    public void onStart(ClientCall call) {
        future.addTimeoutListener(
            () -> call.cancelByLocal(new IllegalStateException("client timeout")));
        call.request(2);
    }
}
