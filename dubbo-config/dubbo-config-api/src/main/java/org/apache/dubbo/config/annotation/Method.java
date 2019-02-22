package org.apache.dubbo.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.dubbo.common.Constants;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface Method {

  /**
   * Timeout value for service invocation, default value is 0
   */
  int timeout() default 0;

  /**
   * Service invocation retry times
   *
   * @see Constants#DEFAULT_RETRIES
   */
  int retries() default Constants.DEFAULT_RETRIES;

  /**
   * Load balance strategy, legal values include: random, roundrobin, leastactive
   *
   * @see Constants#DEFAULT_LOADBALANCE
   */
  String loadbalance() default Constants.DEFAULT_LOADBALANCE;

  /**
   * Whether to enable async invocation, default value is false
   */
  boolean async() default false;

  /**
   * Maximum active requests allowed, default value is 0
   */
  int actives() default 0;

  /**
   * Whether the async request has already been sent, the default value is false
   */
  boolean sent() default false;

  /**
   * Maximum concurrent executes for the service, default value is 0 - no limits
   */
  int executes() default 0;

  /**
   * Whether the service is deprecated, default value is false
   */
  boolean deprecated() default false;

  /**
   * Enable/Disable cluster sticky policy, default value is false.
   */
  boolean sticky() default false;

  /**
   * Method result need return, when async is true.default value is true.
   */
  boolean needReturn() default true;

  /**
   * Specify cache implementation for service invocation, legal values include: lru, threadlocal, jcache
   */
  String cache() default "";

  /**
   * Whether to use JSR303 validation, legal values are: true, false
   */
  boolean validation() default false;

  /**
   * Method return trigger. return attribute must be true
   */
  String onReturn() default "";

  /**
   * Method invoke trigger.
   */
  String onInvoke() default "";

  /**
   * Method on error trigger.return attribute must be true.
   */
  String onThrow() default "";
}
