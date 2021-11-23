package org.apache.dubbo.rpc.cluster.router.mesh.route;

import org.apache.dubbo.common.URL;

import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.STANDARD_ROUTER_KEY;

public class StandardMeshRuleRouter<T> extends MeshRuleRouter<T> {

    public StandardMeshRuleRouter(URL url) {
        super(url);
    }

    @Override
    public String ruleSuffix() {
        return STANDARD_ROUTER_KEY;
    }

    @Override
    public boolean isForce() {
        return false;
    }

    @Override
    public int getPriority() {
        return -500;
    }
}
