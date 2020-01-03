package org.apache.dubbo.config;

import org.apache.dubbo.common.extension.SPI;

/**
 * 2019/12/30
 */

@SPI
public interface ConfigPostProcessor {

    default void postProcessReferConfig(ReferenceConfig referenceConfig) {

    }

    default void postProcessServiceConfig(ServiceConfig serviceConfig) {

    }
}
