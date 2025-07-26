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
package com.alibaba.dubbo.rpc;

import java.util.Map;

/**
 * RPC invoke result. (API, Prototype, NonThreadSafe)
 *
 * @serial Don't change the class name and package name.
 * @see com.alibaba.dubbo.rpc.Invoker#invoke(Invocation)
 * @see com.alibaba.dubbo.rpc.RpcResult
 * 是会话域，持有调用过程中返回值。
 */
public interface Result {

    /**
     * Get invoke result.
     *
     * @return result. if no result return null.
     * 获得返回值
     */
    Object getValue();

    /**
     * Get exception.
     *
     * @return exception. if no exception return null.
     * 获得返回的异常
     */
    Throwable getException();

    /**
     * Has exception.
     *
     * @return has exception.
     * 是否有异常
     */
    boolean hasException();

    /**
     * Recreate.
     * <p>
     * <code>
     * if (hasException()) {
     * throw getException();
     * } else {
     * return getValue();
     * }
     * </code>
     *
     * @return result.
     * @throws if has exception throw it.
     */
    Object recreate() throws Throwable;

    /**
     * @see com.alibaba.dubbo.rpc.Result#getValue()
     * @deprecated Replace to getValue()
     */
    @Deprecated
    Object getResult();


    /**
     * get attachments.
     *
     * @return attachments.
     * 获得返回的隐式参数列表。
     */
    Map<String, String> getAttachments();

    /**
     * get attachment by key.
     *
     * @return attachment value.
     */
    String getAttachment(String key);

    /**
     * get attachment by key with default value.
     *
     * @return attachment value.
     */
    String getAttachment(String key, String defaultValue);

}