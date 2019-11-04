package org.apache.dubbo.samples.microservices.sc.feign;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "dubbo-provider-demo")
public interface TestFeign {

    @RequestMapping(value="/users/1", method = RequestMethod.GET)
    String doAlive();

}
