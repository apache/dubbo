package org.apache.dubbo.common.serialize.fastjson2;

import org.apache.dubbo.common.utils.AllowClassNotifyListener;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Set;

public class Fastjson2AllowClassNotifyListener implements AllowClassNotifyListener {
    private final FrameworkModel frameworkModel;

    public Fastjson2AllowClassNotifyListener(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public void notify(Set<String> prefixList) {
        Fastjson2SecurityManager fastjson2SecurityManager = frameworkModel.getBeanFactory().getBean(Fastjson2SecurityManager.class);
        fastjson2SecurityManager.notify(prefixList);
    }
}
