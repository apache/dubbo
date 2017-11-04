package com.alibaba.dubbo.config.spring.context.annotation.provider;

import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;

import org.springframework.context.annotation.ImportResource;

/**
 * @author ken.lj
 * @date 2017/11/3
 */
@DubboComponentScan(basePackages = "com.alibaba.dubbo.config.spring.context.annotation")
@ImportResource("META-INF/spring/dubbo-annotation-provider.xml")
public class ProviderConfiguration {


}

