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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;

import io.netty.util.AsciiString;

import java.util.Map;

public class RequestMetadata {
    public long requestId;
    public AsciiString scheme;
    public String application;
    public String service;
    public String version;
    public String group;
    public String address;
    public String acceptEncoding;
    public String timeout;
    public Compressor compressor;
    public MethodDescriptor method;
    public Object[] arguments;
    public Map<String, Object> attachments;
    public GenericPack genericPack;
    public GenericUnpack genericUnpack;
}
