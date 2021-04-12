package org.apache.dubbo.common.constants;

/**
 *  constant for Cluster fault-tolerant mode
 * @author lucky-pan
 */
public interface ClusterRules {

    /**
     *  When invoke fails, log the initial error and retry other invokers
     *  (retry n times, which means at most n different invokers will be invoked)
     **/
    String FAIL_OVER = "failover";

    /**
     *  Execute exactly once, which means this policy will throw an exception immediately in case of an invocation error.
     **/
    String FAIL_FAST = "failfast";

    /**
     *  When invoke fails, log the error message and ignore this error by returning an empty Result.
     **/
    String FAIL_SAFE = "failsafe";

    /**
     *  When fails, record failure requests and schedule for retry on a regular interval.
     **/
    String FAIL_BACK = "failback";

    /**
     *  Invoke a specific number of invokers concurrently, usually used for demanding real-time operations, but need to waste more service resources.
     **/
    String FORKING = "forking";

    /**
     *  Call all providers by broadcast, call them one by one, and report an error if any one reports an error
     **/
    String BROADCAST = "broadcast";


    String AVAILABLE = "available";

    String MERGEABLE = "mergeable";

    String EMPTY = "";


}
