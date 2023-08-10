package org.apache.dubbo.config.deploy.lifecycle.context;

import org.apache.dubbo.common.deploy.DeployListenable;
import org.apache.dubbo.common.deploy.DeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.List;

public interface ModelContext<T extends ScopeModel> extends DeployListenable<T> {

    T getModel();

    List<DeployListener<T>> getListeners();

    DeployState getCurrentState();

    void setModelState(DeployState newState);

    Throwable getLastError();

    void setLastError(Throwable lastError);

    boolean initialized();

    void setInitialized(boolean initialized);

}

