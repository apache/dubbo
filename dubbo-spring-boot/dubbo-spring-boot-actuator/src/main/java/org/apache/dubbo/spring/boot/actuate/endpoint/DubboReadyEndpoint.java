package org.apache.dubbo.spring.boot.actuate.endpoint;

import org.apache.dubbo.spring.boot.actuate.endpoint.metadata.DubboReadyMetadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.Map;

/**
 * Dubbo Ready
 *
 * @since 3.3.0
 */
@Endpoint(id = "dubboready")
public class DubboReadyEndpoint extends DubboReadyMetadata {

    @Autowired
    private DubboReadyMetadata dubboReadyMetadata;

    @ReadOperation
    public Map<String, Object> ready() {
        return dubboReadyMetadata.ready();
    }
}
