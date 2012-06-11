/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.monitor;

import com.alibaba.dubbo.common.URL;

/**
 * MonitorService. (SPI, Prototype, ThreadSafe)
 * 
 * @author william.liangf
 */
public interface MonitorService {
    
    String APPLICATION = "application";
    
    String INTERFACE = "interface";

    String METHOD = "method";

    String GROUP = "group";

    String VERSION = "version";

    String CONSUMER = "consumer";

    String PROVIDER = "provider";
    
    String TIMESTAMP = "timestamp";

    String SUCCESS = "success";

    String FAILURE = "failure";
    
    String INPUT = "input";

    String OUTPUT = "output";

    String ELAPSED = "elapsed";

    String CONCURRENT = "concurrent";

    String MAX_INPUT = "max.input";

    String MAX_OUTPUT = "max.output";

    String MAX_ELAPSED = "max.elapsed";

    String MAX_CONCURRENT = "max.concurrent";

    /**
     * collect.
     * 
     * @param statistics
     */
    void collect(URL statistics);
    
}