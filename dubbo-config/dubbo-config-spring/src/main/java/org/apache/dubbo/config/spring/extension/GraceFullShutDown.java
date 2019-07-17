package org.apache.dubbo.config.spring.extension;

/**
 * this interface is used to enable grace shutdown
 * any class implements this interface should be a spring bean
 * @author tiantian.yuan
 * @date 2018年12月24日21:12:35
 */
public interface GraceFullShutDown {


    /**
     * it is the beginning stage of shut down
     * some resources should be closed after  dubbo registries are destroyed but before dubbo connections are destroyed
     * so you should do something in close method
     * at this moment,some dubbo rpc invoke may be executing now,so any destroy method for are forbidden
     * for example, kafka message consumer should stop the poll method at this stage
     */
    default void afterRegistriesDestroyed(){}

    /**
     * it is the last stage for shut down
     * some operations should be done after dubbo connections destroyed
     * for example,some cache should be flush to db,or some objects with spring lifecycle should be destroyed
     */
    default void afterProtocolDestroyed(){}
}
