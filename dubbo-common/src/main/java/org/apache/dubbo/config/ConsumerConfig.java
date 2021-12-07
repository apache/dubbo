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
package org.apache.dubbo.config;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ModuleModel;

import static org.apache.dubbo.common.constants.CommonConstants.REFER_BACKGROUND_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFER_THREAD_NUM_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.URL_MERGE_PROCESSOR_KEY;

/**
 * The service consumer default configuration
 *
 * @export
 */
public class ConsumerConfig extends AbstractReferenceConfig {

    private static final long serialVersionUID = 2827274711143680600L;

    /**
     * Networking framework client uses: netty, mina, etc.
     */
    private String client;

    /**
     * Consumer thread pool type: cached, fixed, limit, eager
     */
    private String threadpool;

    /**
     * Consumer threadpool core thread size
     */
    private Integer corethreads;

    /**
     * Consumer threadpool thread size
     */
    private Integer threads;

    /**
     * Consumer threadpool queue size
     */
    private Integer queues;

    /**
     * By default, a TCP long-connection communication is shared between the consumer process and the provider process.
     * This property can be set to share multiple TCP long-connection communications. Note that only the dubbo protocol takes effect.
     */
    private Integer shareconnections;

    /**
     *  Url Merge Processor
     *  Used to customize the URL merge of consumer and provider
     */
    private String urlMergeProcessor;

    /**
     * Thread num for asynchronous refer pool size
     */
    private Integer referThreadNum;

    /**
     * Whether refer should run in background or not.
     *
     * @deprecated replace with {@link ModuleConfig#setBackground(Boolean)}
     * @see ModuleConfig#setBackground(Boolean)
     */
    private Boolean referBackground;


    public ConsumerConfig() {
    }

    public ConsumerConfig(ModuleModel moduleModel) {
        super(moduleModel);
    }

    @Override
    public void setTimeout(Integer timeout) {
        super.setTimeout(timeout);
        String rmiTimeout = System.getProperty("sun.rmi.transport.tcp.responseTimeout");
        if (timeout != null && timeout > 0
                && (StringUtils.isEmpty(rmiTimeout))) {
            System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(timeout));
        }
    }
    
    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(String threadpool) {
        this.threadpool = threadpool;
    }

    public Integer getCorethreads() {
        return corethreads;
    }

    public void setCorethreads(Integer corethreads) {
        this.corethreads = corethreads;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getQueues() {
        return queues;
    }

    public void setQueues(Integer queues) {
        this.queues = queues;
    }

    public Integer getShareconnections() {
        return shareconnections;
    }

    public void setShareconnections(Integer shareconnections) {
        this.shareconnections = shareconnections;
    }

    @Parameter(key = URL_MERGE_PROCESSOR_KEY)
    public String getUrlMergeProcessor() {
        return urlMergeProcessor;
    }

    public void setUrlMergeProcessor(String urlMergeProcessor) {
        this.urlMergeProcessor = urlMergeProcessor;
    }

    @Parameter(key = REFER_THREAD_NUM_KEY, excluded = true)
    public Integer getReferThreadNum() {
        return referThreadNum;
    }

    public void setReferThreadNum(Integer referThreadNum) {
        this.referThreadNum = referThreadNum;
    }

    @Deprecated
    @Parameter(key = REFER_BACKGROUND_KEY, excluded = true)
    public Boolean getReferBackground() {
        return referBackground;
    }

    /**
     * Whether refer should run in background or not.
     *
     * @deprecated replace with {@link ModuleConfig#setBackground(Boolean)}
     * @see ModuleConfig#setBackground(Boolean)
     */
    @Deprecated
    public void setReferBackground(Boolean referBackground) {
        this.referBackground = referBackground;
    }
}
