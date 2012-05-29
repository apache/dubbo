/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.dubbo.rpc.filter.tps;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public interface TPSLimiter {

    /**
     * 根据 tps 限流规则判断是否限制此次调用.
     *
     * @param url url
     * @param invocation invocation
     * @return true 则允许调用，否则不允许
     */
    boolean isAllowable(URL url, Invocation invocation);

}
