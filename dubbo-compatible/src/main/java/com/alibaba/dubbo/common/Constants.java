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

package com.alibaba.dubbo.common;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.FilterConstants;
import org.apache.dubbo.common.constants.QosConstants;
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.constants.RemotingConstants;

@Deprecated
public class Constants implements CommonConstants,
        QosConstants,
        FilterConstants,
        RegistryConstants,
        RemotingConstants,
        org.apache.dubbo.config.Constants,
        org.apache.dubbo.remoting.Constants,
        org.apache.dubbo.rpc.cluster.Constants,
        org.apache.dubbo.monitor.Constants,
        org.apache.dubbo.rpc.Constants,
        org.apache.dubbo.rpc.protocol.dubbo.Constants,
        org.apache.dubbo.common.serialize.Constants,
        org.apache.dubbo.common.config.configcenter.Constants,
        org.apache.dubbo.metadata.report.support.Constants ,
        org.apache.dubbo.rpc.protocol.rest.Constants,
        org.apache.dubbo.registry.Constants {
}
