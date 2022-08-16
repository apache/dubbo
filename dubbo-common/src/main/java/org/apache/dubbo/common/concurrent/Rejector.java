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
package org.apache.dubbo.common.concurrent;

import java.util.Queue;

/**
 * RejectHandler, it works when you need to custom reject action.
 *
 * @see AbortPolicy
 * @see DiscardPolicy
 * @see DiscardOldestPolicy
 */
public interface Rejector<E> {

    /**
     * Method that may be invoked by a Queue when Queue has remained memory
     * return true. This may occur when no more memory are available because their bounds would be exceeded.
     *
     * <p>In the absence of other alternatives, the method may throw an unchecked
     * {@link RejectException}, which will be propagated to the caller.
     *
     * @param e     the element requested to be added
     * @param queue the queue attempting to add this element
     *
     * @throws RejectException if there is no more memory
     */
    void reject(E e, Queue<E> queue);
}
