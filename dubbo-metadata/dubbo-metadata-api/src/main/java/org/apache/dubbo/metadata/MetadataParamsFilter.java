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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.extension.SPI;

/**
 * This filter applies an either 'include' or 'exclude' policy with 'include' having higher priority.
 * That means if 'include' is specified then params specified in 'exclude' will be ignored
 *
 * If multiple Filter extensions are provided, then,
 * 1. All params specified as should be included within different Filter extension instances will determine the params that will finally be used.
 * 2. If none of the Filter extensions specified any params as should be included, then the final effective params would be those left after removed all the params specified as should be excluded.
 *
 * It is recommended for most users to use 'exclude' policy for service params and 'include' policy for instance params.
 * Please use 'params-filter=-default, -filterName1, filterName2' to activate or deactivate filter extensions.
 */
@SPI
public interface MetadataParamsFilter {

   /**
    * params that need to be sent to metadata center
    *
    * @return arrays of keys
    */
   default String[] serviceParamsIncluded() {
       return new String[0];
   }

    /**
     * params that need to be excluded before sending to metadata center
     *
     * @return arrays of keys
     */
    default String[] serviceParamsExcluded() {
        return new String[0];
    }

    /**
     * params that need to be sent to registry center
     *
     * @return arrays of keys
     */
    default String[] instanceParamsIncluded() {
        return new String[0];
    }

    /**
     * params that need to be excluded before sending to registry center
     *
     * @return arrays of keys
     */
    default String[] instanceParamsExcluded() {
        return new String[0];
    }
}
