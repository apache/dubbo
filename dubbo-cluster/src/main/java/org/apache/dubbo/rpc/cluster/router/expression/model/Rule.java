package org.apache.dubbo.rpc.cluster.router.expression.model;

/**
 * A single rule which client prerequisite and server filter.
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
