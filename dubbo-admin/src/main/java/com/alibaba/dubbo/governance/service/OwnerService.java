package com.alibaba.dubbo.governance.service;

import com.alibaba.dubbo.registry.common.domain.Owner;

import java.util.List;

public interface OwnerService {

    List<String> findAllServiceNames();

    List<String> findServiceNamesByUsername(String username);

    List<String> findUsernamesByServiceName(String serviceName);

    List<Owner> findByService(String serviceName);

    List<Owner> findAll();

    Owner findById(Long id);

    void saveOwner(Owner owner);

    void deleteOwner(Owner owner);

}
