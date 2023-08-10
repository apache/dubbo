package org.apache.dubbo.config.deploy.lifecycle.context;

import org.apache.dubbo.rpc.model.ModuleModel;

public class ModuleContext extends AbstractModelContext<ModuleModel> {
    public ModuleContext(ModuleModel scopeModel) {
        super(scopeModel);
    }
}
