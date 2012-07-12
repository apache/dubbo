package com.alibaba.dubbo.governance.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.OwnerService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.registry.common.domain.Consumer;
import com.alibaba.dubbo.registry.common.domain.Owner;
import com.alibaba.dubbo.registry.common.domain.Provider;

public class OwnerServiceImpl extends AbstractService implements OwnerService {
    
    @Autowired
    ProviderService providerService;
    
    @Autowired
    ConsumerService consumerService;

    public List<String> findAllServiceNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> findServiceNamesByUsername(String username) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> findUsernamesByServiceName(String serviceName) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Owner> findByService(String serviceName) {
        List<Provider> pList = providerService.findByService(serviceName);
        
        List<Consumer> cList = consumerService.findByService(serviceName);
       
        return toOverrideLiset(pList,cList);
    }

    public List<Owner> findAll() {
        List<Provider> pList = providerService.findAll();
       
        List<Consumer> cList = consumerService.findAll();
       
        return toOverrideLiset(pList,cList);
    }

    public Owner findById(Long id) {
       
        return null;
    }
    
    private List<Owner> toOverrideLiset(List<Provider> pList , List<Consumer> cList){
        List<Owner> oList = new ArrayList<Owner>();
        for(Provider p : pList){
            if(p.getUsername() != null){
                Owner o = new Owner();
                o.setService(p.getService());
                o.setUsername(p.getUsername());
                oList.add(o);
            }
        }
        
        for(Consumer c : cList){
            if(c.getUsername() != null){
                Owner o = new Owner();
                o.setService(c.getService());
                o.setUsername(c.getUsername());
                oList.add(o);
            }
        }
        return oList;
    }

}
