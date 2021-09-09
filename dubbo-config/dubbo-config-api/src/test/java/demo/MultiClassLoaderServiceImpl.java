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
package demo;

import java.util.concurrent.atomic.AtomicReference;

public class MultiClassLoaderServiceImpl implements MultiClassLoaderService {
    private AtomicReference<MultiClassLoaderServiceRequest> innerRequestReference;
    private AtomicReference<MultiClassLoaderServiceResult> innerResultReference;

    public MultiClassLoaderServiceImpl(AtomicReference<MultiClassLoaderServiceRequest> innerRequestReference, AtomicReference<MultiClassLoaderServiceResult> innerResultReference) {
        this.innerRequestReference = innerRequestReference;
        this.innerResultReference = innerResultReference;
    }

    @Override
    public MultiClassLoaderServiceResult call(MultiClassLoaderServiceRequest innerRequest) {
        innerRequestReference.set(innerRequest);
        return innerResultReference.get();
    }
}
