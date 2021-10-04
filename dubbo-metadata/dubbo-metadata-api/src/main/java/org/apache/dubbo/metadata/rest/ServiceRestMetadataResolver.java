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
package org.apache.dubbo.metadata.rest;

/**
 * The interface to resolve the {@link ServiceRestMetadata REST metadata} from the specified
 * Dubbo Service interface or type.
 *
 * @since 2.7.6
 */
public interface ServiceRestMetadataResolver {

    /**
     * Support to resolve {@link ServiceRestMetadata REST metadata} or not
     *
     * @param serviceType Dubbo Service interface or type
     * @return If supports, return <code>true</code>, or <code>false</code>
     */
    boolean supports(Class<?> serviceType);

    /**
     * Resolve the {@link ServiceRestMetadata REST metadata} from the specified
     * Dubbo Service interface or type
     *
     * @param serviceType Dubbo Service interface or type
     * @return
     */
    ServiceRestMetadata resolve(Class<?> serviceType);
}
