package org.apache.dubbo.common.deploy;

import org.apache.dubbo.rpc.model.ScopeModel;

public interface DeployListenable<T extends ScopeModel> {
    void addDeployListener(DeployListener<T> listener);

    void removeDeployListener(DeployListener<T> listener);
}
