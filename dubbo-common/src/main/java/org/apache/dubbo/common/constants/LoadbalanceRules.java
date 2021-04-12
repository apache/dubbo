package org.apache.dubbo.common.constants;

/**
 *  constant for Loadbalance strategy
 * @author lucky-pan
 */
public interface LoadbalanceRules {

    /**
     *  This class select one provider from multiple providers randomly.
     **/
    String RANDOM = "random";

    /**
     *  Round robin load balance.
     **/
    String ROUND_ROBIN = "roundrobin";

    /**
     *  Filter the number of invokers with the least number of active calls and count the weights and quantities of these invokers.
     **/
    String LEAST_ACTIVE = "leastactive";

    /**
     *  Consistent Hash, requests with the same parameters are always sent to the same provider.
     **/
    String CONSISTENT_HASH = "consistenthash";

    /**
     *  Filter the number of invokers with the shortest response time of success calls and count the weights and quantities of these invokers.
     **/
    String SHORTEST_RESPONSE = "shortestresponse";

    String EMPTY = "";

}
