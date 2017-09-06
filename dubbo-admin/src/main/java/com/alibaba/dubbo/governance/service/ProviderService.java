/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-4-15
 * $Id: ProviderService.java 182143 2012-06-27 03:25:50Z tony.chenl $
 * <p>
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.service;

import com.alibaba.dubbo.registry.common.domain.Provider;

import java.util.List;

/**
 * ProviderService
 *
 * @author william.liangf
 */
public interface ProviderService {

    void create(Provider provider);

    void enableProvider(Long id);

    void disableProvider(Long id);

    void doublingProvider(Long id);

    void halvingProvider(Long id);

    void deleteStaticProvider(Long id);

    void updateProvider(Provider provider);

    Provider findProvider(Long id);

    List<String> findServices();

    List<String> findAddresses();

    List<String> findAddressesByApplication(String application);

    List<String> findAddressesByService(String serviceName);

    List<String> findApplicationsByServiceName(String serviceName);

    List<Provider> findByService(String serviceName);

    List<Provider> findAll();

    List<Provider> findByAddress(String providerAddress);

    List<String> findServicesByAddress(String providerAddress);

    List<String> findApplications();

    List<Provider> findByApplication(String application);

    List<String> findServicesByApplication(String application);

    List<String> findMethodsByService(String serviceName);

    Provider findByServiceAndAddress(String service, String address);

}