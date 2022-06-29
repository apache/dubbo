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

package org.apache.dubbo.rpc.protocol.tri.service;

import io.grpc.reflection.v1.DubboServerReflectionTriple;

/**
 * This class should replace  v1alpha version when client supports v1 version api.
 *
 * @link https://github.com/grpc/grpc/blob/master/doc/server-reflection.md
 * @see ReflectionV1AlphaService
 */
public class ReflectionService extends DubboServerReflectionTriple.ServerReflectionImplBase {

}
