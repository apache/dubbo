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

import java.util.List;

import com.alibaba.dubbo.common.Constants;
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
    
    String INPUT = Constants.INPUT_KEY;

    String OUTPUT = Constants.OUTPUT_KEY;

    String ELAPSED = "elapsed";

    String CONCURRENT = "concurrent";

    String MAX_INPUT = "max.input";

    String MAX_OUTPUT = "max.output";

    String MAX_ELAPSED = "max.elapsed";

    String MAX_CONCURRENT = "max.concurrent";

    /**
     * 监控数据采集.
     * 1. 支持调用次数统计：count://host/interface?application=foo&method=foo&provider=10.20.153.11:20880&success=12&failure=2&elapsed=135423423
     * 1.1 host,application,interface,group,version,method 记录监控来源主机，应用，接口，方法信息。
     * 1.2 如果是消费者发送的数据，加上provider地址参数，反之，加上来源consumer地址参数。
     * 1.3 success,faulure,elapsed 记录距上次采集，调用的成功次数，失败次数，成功调用总耗时，平均时间将用总耗时除以成功次数。
     * 
     * @param statistics
     */
    void collect(URL statistics);

    /**
     * 监控数据查询. 
     * 1. 支持按天查询：count://host/interface?application=foo&method=foo&side=provider&view=chart&date=2012-07-03
     * 1.1 host,application,interface,group,version,method 查询主机，应用，接口，方法的匹配条件，缺失的条件的表示全部，host用0.0.0.0表示全部。
     * 1.2 side=consumer,provider 查询由调用的哪一端采集的数据，缺省为都查询。
     * 1.3 缺省为view=summary，返回全天汇总信息，支持view=chart表示返回全天趋势图表图片的URL地址，可以进接嵌入其它系统的页面上展示。
     * 1.4 date=2012-07-03 指定查询数据的日期，缺省为当天。
     * 
     * @param query
     * @return statistics
     */
    List<URL> lookup(URL query);

}