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
import org.apache.dubbo.auth.model.AccessKeyPair;
import org.apache.dubbo.auth.spi.AccessKeyStorage;
import org.apache.dubbo.auth.spi.AuthenticationHelper;
import org.apache.dubbo.auth.utils.SignatureUtils;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;

public class AccessKeyAuthenticationHelper implements AuthenticationHelper {
    @Override
    public void signForRequest(Invocation invocation, URL url) {
        String currentTime = String.valueOf(System.currentTimeMillis());
        String consumer = url.getParameter(CommonConstants.APPLICATION_KEY);
        AccessKeyPair accessKeyPair = getAccessKeyPair(invocation, url);
        invocation.setAttachment(Constants.SIGNATURE_STRING_FORMAT, getSignature(url, invocation, accessKeyPair.getSecretKey(), currentTime));
        invocation.setAttachment(Constants.REQUEST_TIMESTAMP_KEY, currentTime);
        invocation.setAttachment(Constants.AK_KEY, accessKeyPair.getAccessKey());
        invocation.setAttachment(CommonConstants.CONSUMER, consumer);
    }

    @Override
    public boolean authenticateRequest(Invocation invocation, URL url) {
        String accessKeyId = String.valueOf(invocation.getAttachment(Constants.AK_KEY));
        String requestTimestamp = String.valueOf(invocation.getAttachment(Constants.REQUEST_TIMESTAMP_KEY));
        String originSignature = String.valueOf(invocation.getAttachment(Constants.REQUEST_SIGNATURE_KEY));
        String consumer = String.valueOf(invocation.getAttachment(CommonConstants.CONSUMER));

        if (StringUtils.isEmpty(accessKeyId) || StringUtils.isEmpty(consumer)
                || StringUtils.isEmpty(requestTimestamp) || StringUtils.isEmpty(originSignature)) {
            throw new RuntimeException("Auth failed, maybe consumer not enable the auth");
        }
        AccessKeyPair accessKeyPair = getAccessKeyPair(invocation, url);

        String computeSignature = getSignature(url, invocation, accessKeyPair.getSecretKey(), requestTimestamp);
        boolean success = computeSignature.equals(originSignature);
        if (!success) {
            throw new RuntimeException("Auth failed, signature is not correct");
        }
        return success;
    }


    AccessKeyPair getAccessKeyPair(Invocation invocation, URL url) {
        AccessKeyStorage accessKeyStorage = ExtensionLoader.getExtensionLoader(AccessKeyStorage.class)
                .getExtension(url.getParameter(Constants.ACCESS_KEY_STORAGE_KEY, Constants.DEFAULT_ACCESS_KEY_STORAGE));

        AccessKeyPair accessKeyPair = null;
        try {
            accessKeyPair = accessKeyStorage.getAccessKey(url, invocation);
            if (accessKeyPair == null || StringUtils.isEmpty(accessKeyPair.getAccessKey()) || StringUtils.isEmpty(accessKeyPair.getSecretKey())) {
                throw new AccessKeyNotFoundException("AccessKeyId or secretAccessKey not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Can't load the AccessKeyPair from accessKeyStorage", e);
        }
        return accessKeyPair;
    }

    String getSignature(URL url, Invocation invocation, String secrectKey, String time) {
        boolean parameterEncrypt = url.getParameter(Constants.PARAMTER_ENCRYPT_ENABLE_KEY, false);
        String signature;
        String requestString = String.format(Constants.SIGNATURE_STRING_FORMAT,
                url.getColonSeparatedKey(), invocation.getMethodName(), secrectKey, time);
        if (parameterEncrypt) {
            signature = SignatureUtils.sign(invocation.getArguments(), requestString, secrectKey);
        } else {
            signature = SignatureUtils.sign(requestString, secrectKey);
        }
        return signature;
    }

}
