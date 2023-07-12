package org.apache.dubbo.rpc.cluster.spi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.SpiMethods;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.rpc.cluster.support.ClusterUtils;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Map;


/**
 * {@link org.apache.dubbo.rpc.cluster.support.ClusterUtils#mergeUrl(URL, Map)}
 */
public class MergeUrl implements SpiMethod {

    @Override
    public SpiMethods methodName() {
        return SpiMethods.mergeUrl;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }

    @Override
    public Object invoke(Object... params) {

        ModuleModel moduleModel = (ModuleModel) params[0];
        URL url = (URL) params[1];
        Map<String,String>  referenceParameters = (Map<String, String>) params[2];

        return  moduleModel.getApplicationModel().getBeanFactory().getBean(ClusterUtils.class).mergeUrl(url, referenceParameters);
    }
}
