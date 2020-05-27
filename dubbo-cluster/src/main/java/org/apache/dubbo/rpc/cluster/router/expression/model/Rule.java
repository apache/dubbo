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
package org.apache.dubbo.rpc.cluster.router.expression.model;

/**
 * A single rule which client prerequisite and server filter.
 *
 * @author Weihua
 * @since 2.7.8
 */
public class Rule {

    /**
     * Somewhat like whenCondition in ConditionRouter.
     * This is acted on client and the result should be true/false after evaluation.
     */
    private String clientCondition;
    /**
     * Somewhat like thenCondition in ConditionRouter.
     * This is acted on server and the result should be server list after evaluation.
     */
    private String serverQuery;

    public String getClientCondition() {
        return clientCondition;
    }

    public void setClientCondition(String clientCondition) {
        this.clientCondition = clientCondition;
    }

    public String getServerQuery() {
        return serverQuery;
    }

    public void setServerQuery(String serverQuery) {
        this.serverQuery = serverQuery;
    }

    public String toString(){
        return "Rule(clientCondition=" + clientCondition
                + ", serverQuery=" + serverQuery + ")";
    }
}
