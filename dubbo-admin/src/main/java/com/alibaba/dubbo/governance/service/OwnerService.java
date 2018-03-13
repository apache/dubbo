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
package com.alibaba.dubbo.governance.service;

import com.alibaba.dubbo.registry.common.domain.Owner;

import java.util.List;

public interface OwnerService {

    List<String> findAllServiceNames();

    List<String> findServiceNamesByUsername(String username);

    List<String> findUsernamesByServiceName(String serviceName);

    List<Owner> findByService(String serviceName);

    List<Owner> findAll();

    Owner findById(Long id);

    void saveOwner(Owner owner);

    void deleteOwner(Owner owner);

}
