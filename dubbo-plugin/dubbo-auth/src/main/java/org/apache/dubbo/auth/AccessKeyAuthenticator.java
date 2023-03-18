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
package org.apache.dubbo.auth;

import org.apache.dubbo.auth.exception.AccessKeyNotFoundException;
import org.apache.dubbo.auth.exception.RpcAuthenticationException;
import org.apache.dubbo.auth.model.AccessKeyPair;
import org.apache.dubbo.auth.spi.AccessKeyStorage;
import org.apache.dubbo.auth.spi.Authenticator;
import org.apache.dubbo.auth.utils.SignatureUtils;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class AccessKeyAuthenticator implements Authenticator {
    private final ApplicationModel applicationModel;

    public AccessKeyAuthenticator(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    public void sign(Invocation invocation, URL url) {
        String currentTime = String.valueOf(System.currentTimeMillis());
        AccessKeyPair accessKeyPair = getAccessKeyPair(invocation, url);
        invocation.setAttachment(Constants.REQUEST_SIGNATURE_KEY, getSignature(url, invocation, accessKeyPair.getSecretKey(), currentTime));
        invocation.setAttachment(Constants.REQUEST_TIMESTAMP_KEY, currentTime);
        invocation.setAttachment(Constants.AK_KEY, accessKeyPair.getAccessKey());
        invocation.setAttachment(CommonConstants.CONSUMER, url.getApplication());
    }

    @Override
    public void authenticate(Invocation invocation, URL url) throws RpcAuthenticationException {
        String accessKeyId = String.valueOf(invocation.getAttachment(Constants.AK_KEY));
        String requestTimestamp = String.valueOf(invocation.getAttachment(Constants.REQUEST_TIMESTAMP_KEY));
        String originSignature = String.valueOf(invocation.getAttachment(Constants.REQUEST_SIGNATURE_KEY));
        String consumer = String.valueOf(invocation.getAttachment(CommonConstants.CONSUMER));
        if (StringUtils.isAnyEmpty(accessKeyId, consumer, requestTimestamp, originSignature)) {
            throw new RpcAuthenticationException("Failed to authenticate, maybe consumer side did not enable the auth");
        }
        
        AccessKeyPair accessKeyPair;
        try {
            accessKeyPair = getAccessKeyPair(invocation, url);
        } catch (Exception e) {
            throw new RpcAuthenticationException("Failed to authenticate , can't load the accessKeyPair", e);
        }

        String computeSignature = getSignature(url, invocation, accessKeyPair.getSecretKey(), requestTimestamp);
        boolean success = computeSignature.equals(originSignature);
        if (!success) {
            throw new RpcAuthenticationException("Failed to authenticate, signature is not correct");
        }
    }

    AccessKeyPair getAccessKeyPair(Invocation invocation, URL url) {
        AccessKeyStorage accessKeyStorage = applicationModel.getExtensionLoader(AccessKeyStorage.class)
                .getExtension(url.getParameter(Constants.ACCESS_KEY_STORAGE_KEY, Constants.DEFAULT_ACCESS_KEY_STORAGE));

        AccessKeyPair accessKeyPair;
        try {
            accessKeyPair = accessKeyStorage.getAccessKey(url, invocation);
            if (accessKeyPair == null || StringUtils.isAnyEmpty(accessKeyPair.getAccessKey(), accessKeyPair.getSecretKey())) {
                throw new AccessKeyNotFoundException("AccessKeyId or secretAccessKey not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Can't load the AccessKeyPair from accessKeyStorage", e);
        }
        return accessKeyPair;
    }

    String getSignature(URL url, Invocation invocation, String secretKey, String time) {
        String requestString = String.format(Constants.SIGNATURE_STRING_FORMAT, url.getColonSeparatedKey(), invocation.getMethodName(), secretKey, time);
        boolean parameterEncrypt = url.getParameter(Constants.PARAMETER_SIGNATURE_ENABLE_KEY, false);
        if (parameterEncrypt) {
            return SignatureUtils.sign(invocation.getArguments(), requestString, secretKey);
        }
        return SignatureUtils.sign(requestString, secretKey);
    }
}
